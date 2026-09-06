package xyz.larkzhh.lime.ui.search.viewmodel

import android.content.Context
import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.glance.appwidget.updateAll
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import xyz.larkzhh.lime.R
import xyz.larkzhh.lime.data.local.SearchHistoryStorage
import xyz.larkzhh.lime.data.network.model.FeedItem
import xyz.larkzhh.lime.data.network.model.HotSearchItem
import xyz.larkzhh.lime.data.network.model.UserSearchItem
import xyz.larkzhh.lime.domain.NoteEvent
import xyz.larkzhh.lime.domain.NoteEventBus
import xyz.larkzhh.lime.domain.model.FollowRelation
import xyz.larkzhh.lime.domain.repository.FollowRepository
import xyz.larkzhh.lime.domain.repository.NoteRepository
import xyz.larkzhh.lime.domain.repository.SearchRepository
import xyz.larkzhh.lime.navigation.Screen
import xyz.larkzhh.lime.ui.widget.SearchWidget
import xyz.larkzhh.lime.ui.widget.WidgetHotCache
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

/// 搜索页模式：默认、输入联想、搜索结果
enum class SearchMode { Idle, Suggest, Result }

/// 笔记排序依据
enum class NoteSort(@StringRes val labelRes: Int, val apiValue: String) {
    Composite(R.string.search_sort_composite, "composite"),
    Latest(R.string.search_sort_latest, "latest"),
    MostLiked(R.string.search_sort_most_liked, "likes"),
    MostCommented(R.string.search_sort_most_commented, "comments"),
    MostFavored(R.string.search_sort_most_favored, "favs"),
}

/// 发布时间筛选
enum class SearchTimeRange(@StringRes val labelRes: Int, val apiValue: String) {
    All(R.string.search_time_all, "all"),
    Day(R.string.search_time_day, "day"),
    Week(R.string.search_time_week, "week"),
    HalfYear(R.string.search_time_half_year, "halfYear"),
}

/// 笔记类型筛选
enum class SearchNoteType(@StringRes val labelRes: Int, val apiValue: String) {
    All(R.string.search_type_all, "all"),
    Image(R.string.search_type_image, "image"),
    Video(R.string.search_type_video, "video"),
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
    // 用户搜索结果
    val userItems: List<UserSearchItem> = emptyList(),
    val isUserLoading: Boolean = false,
    val isUserLoadingMore: Boolean = false,
    val userHasMore: Boolean = true,
    val userError: String? = null,
    // 筛选
    val sort: NoteSort = NoteSort.Composite,
    val timeRange: SearchTimeRange = SearchTimeRange.All,
    val noteType: SearchNoteType = SearchNoteType.All,
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
    private val followRepository: FollowRepository,
    @ApplicationContext private val appContext: Context,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState(history = historyStorage.load()))
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    /// 共享关注关系
    val relations: StateFlow<Map<Long, FollowRelation>> = followRepository.relations
    private val suggestQuery = MutableStateFlow("")// 联想去抖动的输入流
    private var resultCursor: String? = null// 笔记结果分页游标
    private var userCursor: String? = null// 用户结果分页游标
    private var loadedUserQuery: String? = null// 用户结关键词，去重用

    init {
        loadHotSearches()
        observeSuggestQuery()
        observeNoteEvents()
        // 详情页进入
        savedStateHandle.get<String>(Screen.Search.ARG_QUERY)
            ?.takeIf { it.isNotBlank() }
            ?.let { confirmSearch(it) }
    }

    /// 加载热搜榜
    private fun loadHotSearches() {
        viewModelScope.launch {
            val cached = WidgetHotCache.read()
            if (cached.isNotEmpty()) {
                _uiState.update { it.copy(hotWords = cached) }
            }
            searchRepository.getHotSearches().onSuccess { hotWords ->
                _uiState.update { it.copy(hotWords = hotWords) }
                WidgetHotCache.write(hotWords)
                runCatching { SearchWidget().updateAll(appContext) }
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
                    is NoteEvent.CommentCountChanged -> Unit
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
        userCursor = null
        loadedUserQuery = null// 清空标记，下次重新请求
        _uiState.update {
            it.copy(
                query = trimmed,
                mode = SearchMode.Result,
                history = historyStorage.load(),
                suggestions = emptyList(),
                userItems = emptyList(),
                userError = null,
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

    /// 切换笔记类型，重置分页并重新搜索
    fun onNoteTypeChange(noteType: SearchNoteType) {
        if (_uiState.value.noteType == noteType) return
        _uiState.update { it.copy(noteType = noteType) }
        if (_uiState.value.mode == SearchMode.Result) loadFirstPage()
    }

    /// 重置筛选条件并重新搜索
    fun resetFilter() {
        _uiState.update {
            it.copy(
                sort = NoteSort.Composite,
                timeRange = SearchTimeRange.All,
                noteType = SearchNoteType.All,
            )
        }
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
                type = state.noteType.apiValue,
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
                type = state.noteType.apiValue,
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

    /// 进入用户结果页，加载第一页
    fun onUserTabEnter() {
        val state = _uiState.value
        if (state.mode != SearchMode.Result) return
        if (loadedUserQuery == state.query) return// 按当前关键词去重
        loadUserFirstPage(state.query)
    }

    /// 加载用户结果第一页
    private fun loadUserFirstPage(query: String) {
        loadedUserQuery = query
        viewModelScope.launch {
            _uiState.update {
                it.copy(isUserLoading = true, userError = null, userItems = emptyList(), userHasMore = true)
            }
            userCursor = null
            searchRepository.searchUsers(keyword = query, cursor = null).fold(
                onSuccess = { response ->
                    userCursor = response.nextCursor
                    _uiState.update {
                        it.copy(
                            isUserLoading = false,
                            userItems = response.items,
                            userHasMore = response.hasMore,
                        )
                    }
                    seedRelations(response.items)
                },
                onFailure = { e ->
                    loadedUserQuery = null
                    _uiState.update { it.copy(isUserLoading = false, userError = e.message) }
                },
            )
        }
    }

    /// 加载更多用户结果
    fun loadMoreUsers() {
        val state = _uiState.value
        if (state.isUserLoadingMore || !state.userHasMore || state.isUserLoading) return
        viewModelScope.launch {
            _uiState.update { it.copy(isUserLoadingMore = true) }
            searchRepository.searchUsers(keyword = state.query, cursor = userCursor).fold(
                onSuccess = { response ->
                    userCursor = response.nextCursor
                    _uiState.update {
                        it.copy(
                            isUserLoadingMore = false,
                            userItems = it.userItems + response.items,
                            userHasMore = response.hasMore,
                        )
                    }
                    seedRelations(response.items)
                },
                onFailure = { e ->
                    _uiState.update { it.copy(isUserLoadingMore = false, userError = e.message) }
                },
            )
        }
    }

    /// 关注用户
    fun followUser(userId: Long) {
        viewModelScope.launch { followRepository.follow(userId) }
    }

    /// 取消关注
    fun unfollowUser(userId: Long) {
        viewModelScope.launch { followRepository.unfollow(userId) }
    }

    /// 写入共享关注关系
    private fun seedRelations(items: List<UserSearchItem>) {
        items.forEach { item ->
            if (item.isMe) return@forEach
            followRepository.updateRelation(
                item.id,
                FollowRelation(
                    following = item.isFollowing ?: false,
                    followedBack = item.isFollowedBack ?: false,
                ),
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
