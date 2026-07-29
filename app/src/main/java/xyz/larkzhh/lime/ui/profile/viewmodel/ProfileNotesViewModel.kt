package xyz.larkzhh.lime.ui.profile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import xyz.larkzhh.lime.data.network.model.FeedItem
import xyz.larkzhh.lime.domain.NoteEvent
import xyz.larkzhh.lime.domain.NoteEventBus
import xyz.larkzhh.lime.domain.repository.NoteRepository
import xyz.larkzhh.lime.domain.repository.UserRepository
import javax.inject.Inject

data class ProfileNotesUiState(
    val items: List<FeedItem> = emptyList(),
    val likedIds: Set<Long> = emptySet(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasMore: Boolean = true,
    val error: String? = null,
)

/**
 * 个人主页笔记列表的 ViewModel。
 * 负责获取当前登录用户的已发布笔记、处理点赞逻辑
 */
@HiltViewModel
class ProfileNotesViewModel @Inject constructor(
    private val noteRepository: NoteRepository,
    private val userRepository: UserRepository,
    private val eventBus: NoteEventBus,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileNotesUiState(isLoading = true))
    val uiState: StateFlow<ProfileNotesUiState> = _uiState.asStateFlow()

    private var cursor: Long? = null
    private var userId: Long? = null

    init {
        viewModelScope.launch {
            /// 等待用户数据
            val user = userRepository.userFlow.filterNotNull().first()
            userId = user.id
            loadNotes()
        }
        observeNoteEvents()
    }

    /// 观察同步详情页的点赞变更
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

    /// 加载笔记
    private fun loadNotes() {
        val uid = userId ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, items = emptyList(), hasMore = true) }
            cursor = null
            noteRepository.getUserNotes(userId = uid, cursor = null).fold(
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

    /// 加载更多
    fun loadMore() {
        val uid = userId ?: return
        val state = _uiState.value
        if (state.isLoadingMore || !state.hasMore || state.isLoading) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true) }
            noteRepository.getUserNotes(userId = uid, cursor = cursor).fold(
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
}
