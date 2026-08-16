package xyz.larkzhh.lime.ui.search.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import xyz.larkzhh.lime.data.local.SearchHistoryStorage
import xyz.larkzhh.lime.data.network.model.FeedItem
import xyz.larkzhh.lime.data.network.model.HotSearchItem
import xyz.larkzhh.lime.domain.NoteEvent
import xyz.larkzhh.lime.domain.NoteEventBus
import xyz.larkzhh.lime.domain.repository.NoteRepository
import xyz.larkzhh.lime.domain.repository.SearchRepository
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

/// 搜索页模式：默认、输入联想、搜索结果
enum class SearchMode { Idle, Suggest, Result }

/// 笔记排序依据
enum class NoteSort(val label: String, val apiValue: String) {
    Composite("综合", "composite"),
    Latest("最新", "latest"),
    MostLiked("最多点赞", "likes"),
    MostCommented("最多评论", "comments"),
    MostFavored("最多收藏", "favs"),
}

/// 发布时间筛选
enum class SearchTimeRange(val label: String, val apiValue: String) {
    All("不限", "all"),
    Day("一天内", "day"),
    Week("一周内", "week"),
    HalfYear("半年内", "halfYear"),
}

data class SearchUiState(
    val mode: SearchMode = SearchMode.Idle,
    val query: String = "",
    val suggestions: List<String> = emptyList(),
    val history: List<String> = emptyList(),
    val hotWords: List<HotSearchItem> = emptyList(),
    // 搜索结果
    val resultItems: List<FeedItem> = emptyList(),
    val likedIds: Set<Long> = emptySet(),
    val isResultLoading: Boolean = false,// 结果首屏加载
    val isLoadingMore: Boolean = false,
    val hasMore: Boolean = true,
    val resultError: String? = null,
    // 筛选
    val sort: NoteSort = NoteSort.Composite,
    val timeRange: SearchTimeRange = SearchTimeRange.All,
)

/**
 * 搜索页 ViewModel。
 * - 管理搜索主页、联想输入态、结果态转换
 * - 联想防抖请求、结果游标分页、点赞状态同步等。
 */
@OptIn(FlowPreview::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchRepository: SearchRepository,
    private val noteRepository: NoteRepository,
    private val historyStorage: SearchHistoryStorage,
    private val eventBus: NoteEventBus,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState(history = historyStorage.load()))
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()
    private val suggestQuery = MutableStateFlow("")// 联想去抖动的输入流
    private var resultCursor: String? = null// 结果分页游标

    init {
        loadHotSearches()
        observeSuggestQuery()
        observeNoteEvents()
    }

    /// 加载热搜榜
    private fun loadHotSearches() {
        viewModelScope.launch {
            searchRepository.getHotSearches().onSuccess { hotWords ->
                _uiState.update { it.copy(hotWords = hotWords) }
            }
        }
    }

    /// 输入防抖
    private fun observeSuggestQuery() {
        viewModelScope.launch {
            suggestQuery
                .debounce(300.milliseconds)// 停顿超过 300 毫秒后收集
                .collect { q ->
                    if (q.isBlank()) return@collect
                    searchRepository.getSuggestions(q.trim()).onSuccess { suggestions ->
                        // 确保响应与当前输入一致，丢弃旧响应
                        if (_uiState.value.query.trim() == q.trim()) {
                            _uiState.update { it.copy(suggestions = suggestions) }
                        }
                    }
                }
        }
    }

    /// 观察、收集事件，更新结果列表的点赞数量与状态
    private fun observeNoteEvents() {
        viewModelScope.launch {
            eventBus.events.collect { event ->
                when (event) {
                    is NoteEvent.LikeChanged -> {
                        _uiState.update { state ->
                            state.copy(
                                resultItems = state.resultItems.map { item ->
                                    if (item.id == event.noteId)
                                        item.copy(liked = event.liked, likeCount = event.likeCount)
                                    else item
                                },
                                likedIds = if (event.liked) state.likedIds + event.noteId
                                else state.likedIds - event.noteId,
                            )
                        }
                    }

                    is NoteEvent.FavoriteChanged -> Unit
                }
            }
        }
    }

    /// 输入变化
    fun onQueryChange(query: String) {
        _uiState.update {
            it.copy(
                query = query,
                mode = if (query.isBlank()) SearchMode.Idle else SearchMode.Suggest,
                suggestions = if (query.isBlank()) emptyList() else it.suggestions,
                // 回到默认页重新同步历史
                history = if (query.isBlank()) historyStorage.load() else it.history,
            )
        }
        suggestQuery.value = query
    }

    /// 清空输入，回到默认模式
    fun clearQuery() = onQueryChange("")

    /// 确认搜索，存入历史、上报热搜统计、进入结果页面并加载第一页
    fun confirmSearch(keyword: String) {
        val trimmed = keyword.trim()
        if (trimmed.isEmpty()) return
        historyStorage.add(trimmed)
        _uiState.update {
            it.copy(
                query = trimmed,
                mode = SearchMode.Result,
                history = historyStorage.load(),
                suggestions = emptyList(),
            )
        }
        viewModelScope.launch { searchRepository.reportSearch(trimmed) }// 上报失败静默
        loadFirstPage()
    }

    /// 结果页面返回，回到搜索主页
    fun backToHome() {
        _uiState.update { it.copy(mode = SearchMode.Idle, history = historyStorage.load()) }
    }

    /// 重新从本地存储同步历史记录
    fun reloadHistory() {
        _uiState.update { it.copy(history = historyStorage.load()) }
    }

    /// 切换排序，重置分页并重新搜索
    fun onSortChange(sort: NoteSort) {
        if (_uiState.value.sort == sort) return
        _uiState.update { it.copy(sort = sort) }
        if (_uiState.value.mode == SearchMode.Result) loadFirstPage()
    }

    /// 切换发布时间，重置分页并重新搜索
    fun onTimeRangeChange(timeRange: SearchTimeRange) {
        if (_uiState.value.timeRange == timeRange) return
        _uiState.update { it.copy(timeRange = timeRange) }
        if (_uiState.value.mode == SearchMode.Result) loadFirstPage()
    }

    /// 重置筛选条件并重新搜索
    fun resetFilter() {
        _uiState.update { it.copy(sort = NoteSort.Composite, timeRange = SearchTimeRange.All) }
        if (_uiState.value.mode == SearchMode.Result) loadFirstPage()
    }

    /// 删除指定历史记录
    fun removeHistory(keyword: String) {
        historyStorage.remove(keyword)
        _uiState.update { it.copy(history = historyStorage.load()) }
    }

    /// 清空历史记录
    fun clearHistory() {
        historyStorage.clear()
        _uiState.update { it.copy(history = emptyList()) }
    }

    /// 加载结果第一页
    private fun loadFirstPage() {
        val state = _uiState.value
        viewModelScope.launch {
            _uiState.update {
                it.copy(isResultLoading = true, resultError = null, resultItems = emptyList(), hasMore = true)
            }
            resultCursor = null
            searchRepository.searchNotes(
                keyword = state.query,
                sort = state.sort.apiValue,
                within = state.timeRange.apiValue,
                cursor = null,
            ).fold(
                onSuccess = { response ->
                    resultCursor = response.nextCursor
                    _uiState.update {
                        it.copy(
                            isResultLoading = false,
                            resultItems = response.items,
                            likedIds = response.items.filter { item -> item.liked }.map { item -> item.id }.toSet(),
                            hasMore = response.hasMore,
                        )
                    }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(isResultLoading = false, resultError = e.message) }
                },
            )
        }
    }

    /// 加载更多结果
    fun loadMore() {
        val state = _uiState.value
        if (state.isLoadingMore || !state.hasMore || state.isResultLoading) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true) }
            searchRepository.searchNotes(
                keyword = state.query,
                sort = state.sort.apiValue,
                within = state.timeRange.apiValue,
                cursor = resultCursor,
            ).fold(
                onSuccess = { response ->
                    resultCursor = response.nextCursor
                    _uiState.update {
                        it.copy(
                            isLoadingMore = false,
                            resultItems = it.resultItems + response.items,
                            likedIds = it.likedIds + response.items.filter { item -> item.liked }.map { item -> item.id }.toSet(),
                            hasMore = response.hasMore,
                        )
                    }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(isLoadingMore = false, resultError = e.message) }
                },
            )
        }
    }

    /// 点赞、取消点赞
    fun toggleLike(noteId: Long) {
        val liked = noteId in _uiState.value.likedIds
        val delta = if (liked) -1 else 1
        _uiState.update {
            it.copy(
                likedIds = if (liked) it.likedIds - noteId else it.likedIds + noteId,
                resultItems = it.resultItems.map { item ->
                    if (item.id == noteId) item.copy(likeCount = item.likeCount + delta) else item
                },
            )
        }
        viewModelScope.launch {
            val result = if (liked) noteRepository.unlikeNote(noteId) else noteRepository.likeNote(noteId)
            result.onFailure {
                _uiState.update {
                    it.copy(
                        likedIds = if (liked) it.likedIds + noteId else it.likedIds - noteId,
                        resultItems = it.resultItems.map { item ->
                            if (item.id == noteId) item.copy(likeCount = item.likeCount - delta) else item
                        },
                    )
                }
            }
        }
    }
}
