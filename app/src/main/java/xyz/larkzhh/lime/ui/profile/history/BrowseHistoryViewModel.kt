package xyz.larkzhh.lime.ui.profile.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import xyz.larkzhh.lime.data.network.model.HistoryFeedItem
import xyz.larkzhh.lime.domain.NoteEvent
import xyz.larkzhh.lime.domain.NoteEventBus
import xyz.larkzhh.lime.domain.repository.NoteRepository
import javax.inject.Inject

data class BrowseHistoryUiState(
    val items: List<HistoryFeedItem> = emptyList(),
    val likedIds: Set<Long> = emptySet(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasMore: Boolean = true,
    val error: String? = null,
    val isManaging: Boolean = false,// 管理模式
    val selectedIds: Set<Long> = emptySet(),
    val isDeleting: Boolean = false,
    val deleteError: String? = null,
)

/**
 * 浏览记录 ViewModel
 * 管理列表数据、选择状态和删除等操作
 */
@HiltViewModel
class BrowseHistoryViewModel @Inject constructor(
    private val noteRepository: NoteRepository,
    private val eventBus: NoteEventBus,
) : ViewModel() {

    private val _uiState = MutableStateFlow(BrowseHistoryUiState(isLoading = true))
    val uiState: StateFlow<BrowseHistoryUiState> = _uiState.asStateFlow()

    private var cursor: Long? = null

    init {
        loadHistory()
        observeNoteEvents()
    }

    /// 观察详情页点赞变更，同步列表状态
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
                }
            }
        }
    }

    /// 加载历史
    fun loadHistory() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, items = emptyList(), hasMore = true) }
            cursor = null
            noteRepository.getHistory(cursor = null).fold(
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

    fun loadMore() {
        val state = _uiState.value
        if (state.isLoadingMore || !state.hasMore || state.isLoading) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true) }
            noteRepository.getHistory(cursor = cursor).fold(
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

    /// 点赞/取消点赞
    fun toggleLike(noteId: Long) {
        val liked = noteId in _uiState.value.likedIds
        val delta = if (liked) -1 else 1
        _uiState.update { state ->
            state.copy(
                likedIds = if (liked) state.likedIds - noteId else state.likedIds + noteId,
                items = state.items.map { item ->
                    if (item.id == noteId) item.copy(likeCount = item.likeCount + delta) else item
                },
            )
        }
        viewModelScope.launch {
            val result = if (liked) noteRepository.unlikeNote(noteId) else noteRepository.likeNote(noteId)
            result.onFailure {
                /// 请求失败时回滚
                _uiState.update { state ->
                    state.copy(
                        likedIds = if (liked) state.likedIds + noteId else state.likedIds - noteId,
                        items = state.items.map { item ->
                            if (item.id == noteId) item.copy(likeCount = item.likeCount - delta) else item
                        },
                    )
                }
            }
        }
    }

    /// 进入管理模式
    fun enterManageMode() {
        _uiState.update { it.copy(isManaging = true, selectedIds = emptySet()) }
    }

    /// 退出管理模式
    fun exitManageMode() {
        _uiState.update { it.copy(isManaging = false, selectedIds = emptySet()) }
    }

    /// 选中笔记
    fun toggleSelect(id: Long) {
        _uiState.update { state ->
            val newSelected = if (id in state.selectedIds) state.selectedIds - id else state.selectedIds + id
            state.copy(selectedIds = newSelected)
        }
    }

    /// 全选/取消全选切换
    fun selectAll() {
        _uiState.update { state ->
            val allIds = state.items.map { it.id }.toSet()
            if (state.selectedIds == allIds) state.copy(selectedIds = emptySet())
            else state.copy(selectedIds = allIds)
        }
    }

    /// 删除已选
    fun deleteSelected() {
        val state = _uiState.value
        val ids = state.selectedIds.toList()
        if (ids.isEmpty()) return
        val isAll = state.selectedIds.size == state.items.size// 判断是否全选
        viewModelScope.launch {
            _uiState.update { it.copy(isDeleting = true, deleteError = null) }
            val result = if (isAll) noteRepository.deleteHistoryAll()
                         else noteRepository.deleteHistory(ids)
            result.fold(
                onSuccess = {
                    _uiState.update { s ->
                        s.copy(
                            isDeleting = false,
                            items = if (isAll) emptyList() else s.items.filter { it.id !in ids },
                            selectedIds = emptySet(),
                            isManaging = false,
                        )
                    }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(isDeleting = false, deleteError = e.message) }
                },
            )
        }
    }

    fun clearDeleteError() {
        _uiState.update { it.copy(deleteError = null) }
    }
}
