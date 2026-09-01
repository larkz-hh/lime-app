package xyz.larkzhh.lime.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import xyz.larkzhh.lime.data.network.model.FeedItem
import xyz.larkzhh.lime.data.network.model.FeedResponse
import xyz.larkzhh.lime.domain.NoteEvent
import xyz.larkzhh.lime.domain.NoteEventBus
import xyz.larkzhh.lime.domain.repository.NoteRepository
import xyz.larkzhh.lime.util.NetworkMonitor
import javax.inject.Inject

data class FeedUiState(
    val items: List<FeedItem> = emptyList(),
    val likedIds: Set<Long> = emptySet(),
    val isLoading: Boolean = false,// 首次加载
    val isRefreshing: Boolean = false,// 下拉刷新
    val isLoadingMore: Boolean = false,
    val hasMore: Boolean = true,
    val error: String? = null,
    val loadMoreError: String? = null,
    val isFromCache: Boolean = false,
    val isOffline: Boolean = false,
)

/**
 * 信息流页面的 ViewModel
 */
@HiltViewModel
class FeedViewModel @Inject constructor(
    private val noteRepository: NoteRepository,
    private val eventBus: NoteEventBus,
    networkMonitor: NetworkMonitor,
) : ViewModel() {

    private val _uiState = MutableStateFlow(FeedUiState(isLoading = true))
    val uiState: StateFlow<FeedUiState> = _uiState.asStateFlow()

    private var cursor: Long? = null// 分页游标

    init {
        loadFeed()
        observeNoteEvents()
        observeNetwork(networkMonitor)
    }

    /// 观察、收集事件，更新点赞数量与状态
    private fun observeNoteEvents() {
        viewModelScope.launch {
            eventBus.events.collect { event ->
                when (event) {
                    is NoteEvent.LikeChanged -> {
                        _uiState.update { state ->
                            state.copy(
                                items = state.items.map { item ->
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

    /// 监听网络状态
    private fun observeNetwork(networkMonitor: NetworkMonitor) {
        viewModelScope.launch {
            networkMonitor.isOnline.collect { online ->
                val wasOffline = _uiState.value.isOffline
                _uiState.update { it.copy(isOffline = !online) }
                if (wasOffline && online) {
                    val s = _uiState.value
                    if (s.items.isEmpty() || s.isFromCache || s.error != null) {
                        refresh()
                    }
                }
            }
        }
    }

    private fun loadFeed() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, hasMore = true) }
            cursor = null
            // 读取读本地缓存
            val cached = noteRepository.getCachedFeedFirstPage()
            if (cached != null) {
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        isFromCache = true,
                        items = cached.items,
                        likedIds = likedIdsOf(cached),
                        hasMore = cached.hasMore,
                    )
                }
            }
            // 网络刷新
            fetchFirstPage(fromCache = cached != null)
        }
    }

    private suspend fun fetchFirstPage(fromCache: Boolean) {
        noteRepository.getFeed(cursor = null).fold(
            onSuccess = { response ->
                cursor = response.nextCursor
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isFromCache = false,
                        items = response.items,
                        likedIds = likedIdsOf(response),
                        hasMore = response.hasMore,
                        error = null,
                    )
                }
            },
            onFailure = { e ->
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            },
        )
    }

    /// 失败重试
    fun retry() {
        if (_uiState.value.isLoading) return
        loadFeed()
    }

    /// 下拉刷新，重新加载，不清空列表
    fun refresh() {
        val state = _uiState.value
        if (state.isRefreshing || state.isLoading) return
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true, error = null) }
            cursor = null
            noteRepository.getFeed(cursor = null).fold(
                onSuccess = { response ->
                    cursor = response.nextCursor
                    _uiState.update {
                        it.copy(
                            isRefreshing = false,
                            isFromCache = false,
                            items = response.items,
                            likedIds = likedIdsOf(response),
                            hasMore = response.hasMore,
                        )
                    }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(isRefreshing = false, error = e.message) }
                },
            )
        }
    }

    fun toggleLike(noteId: Long) {
        val liked = noteId in _uiState.value.likedIds
        val delta = if (liked) -1 else 1
        _uiState.update {
            it.copy(
                likedIds = if (liked) it.likedIds - noteId else it.likedIds + noteId,
                items = it.items.map { item ->
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
                        items = it.items.map { item ->
                            if (item.id == noteId) item.copy(likeCount = item.likeCount - delta) else item
                        },
                    )
                }
            }
        }
    }

    fun loadMore() {
        val state = _uiState.value
        if (state.isLoadingMore || !state.hasMore || state.isLoading) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true, loadMoreError = null) }
            noteRepository.getFeed(cursor = cursor).fold(
                onSuccess = { response ->
                    cursor = response.nextCursor
                    _uiState.update {
                        it.copy(
                            isLoadingMore = false,
                            items = it.items + response.items,
                            likedIds = it.likedIds + response.items.filter { item -> item.liked }.map { item -> item.id }.toSet(),
                            hasMore = response.hasMore,
                        )
                    }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(isLoadingMore = false, loadMoreError = e.message) }
                },
            )
        }
    }

    private fun likedIdsOf(response: FeedResponse): Set<Long> =
        response.items.filter { it.liked }.map { it.id }.toSet()
}
