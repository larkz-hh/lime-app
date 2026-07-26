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
import xyz.larkzhh.lime.domain.repository.NoteRepository
import javax.inject.Inject

data class FeedUiState(
    val items: List<FeedItem> = emptyList(),
    val likedIds: Set<Long> = emptySet(),
    val isLoading: Boolean = false,// 首次加载
    val isRefreshing: Boolean = false,// 下拉刷新
    val isLoadingMore: Boolean = false,
    val hasMore: Boolean = true,
    val error: String? = null,
)

/**
 * 信息流页面的 ViewModel。
 * 负责管理页面的 UI 状态、处理首次加载以及分页加载更多的业务逻辑。
 */
@HiltViewModel
class FeedViewModel @Inject constructor(
    private val noteRepository: NoteRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(FeedUiState(isLoading = true))
    val uiState: StateFlow<FeedUiState> = _uiState.asStateFlow()

    private var cursor: Long? = null// 分页游标

    init {
        loadFeed()
    }

    private fun loadFeed() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, items = emptyList(), hasMore = true) }
            cursor = null
            noteRepository.getFeed(cursor = null).fold(
                onSuccess = { response ->
                    cursor = response.nextCursor
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            items = response.items,
                            likedIds = response.items.filter { item -> item.liked }.map { item -> item.id }.toSet(),
                            hasMore = response.hasMore,
                        )
                    }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                },
            )
        }
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
                            items = response.items,
                            likedIds = response.items.filter { item -> item.liked }.map { item -> item.id }.toSet(),
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
            _uiState.update { it.copy(isLoadingMore = true) }
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
                    _uiState.update { it.copy(isLoadingMore = false, error = e.message) }
                },
            )
        }
    }
}
