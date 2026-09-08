package xyz.larkzhh.lime.ui.profile.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import xyz.larkzhh.lime.data.network.model.FeedItem
import xyz.larkzhh.lime.domain.NoteEvent
import xyz.larkzhh.lime.domain.NoteEventBus
import xyz.larkzhh.lime.domain.repository.NoteRepository
import xyz.larkzhh.lime.domain.repository.UserRepository
import javax.inject.Inject

data class ProfileLikeState(
    val likeStates: Map<Long, Boolean> = emptyMap(),
    val likeCounts: Map<Long, Int> = emptyMap(),
)

/**
 * 个人主页内容 ViewModel，
 * 统一管理笔记、点赞、收藏三个 tab 的列表数据和操作
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ProfileNotesViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val noteRepository: NoteRepository,
    private val userRepository: UserRepository,
    private val eventBus: NoteEventBus,
) : ViewModel() {

    private val _likeState = MutableStateFlow(ProfileLikeState())
    val likeState: StateFlow<ProfileLikeState> = _likeState.asStateFlow()

    /// 提取路由参数中的目标用户id
    private val requestedUserId: Long? = savedStateHandle["userId"]

    /// 解析后的目标用户id
    private val userIdFlow = MutableStateFlow<Long?>(null)

    val notesPager: Flow<PagingData<FeedItem>> = userIdFlow.filterNotNull().flatMapLatest { uid ->
        noteRepository.userNotesPager(uid)
    }.cachedIn(viewModelScope)

    val likesPager: Flow<PagingData<FeedItem>> = userIdFlow.filterNotNull().flatMapLatest { uid ->
        noteRepository.userLikesPager(uid)
    }.cachedIn(viewModelScope)

    val favoritesPager: Flow<PagingData<FeedItem>> = userIdFlow.filterNotNull().flatMapLatest { uid ->
        noteRepository.userFavoritesPager(uid)
    }.cachedIn(viewModelScope)

    init {
        viewModelScope.launch {
            userIdFlow.value = requestedUserId ?: userRepository.userFlow.filterNotNull().first().id
        }
        observeNoteEvents()
    }

    private fun observeNoteEvents() {
        viewModelScope.launch {
            eventBus.events.collect { event ->
                when (event) {
                    is NoteEvent.LikeChanged -> _likeState.update {
                        it.copy(
                            likeStates = it.likeStates + (event.noteId to event.liked),
                            likeCounts = it.likeCounts + (event.noteId to event.likeCount),
                        )
                    }
                    is NoteEvent.FavoriteChanged -> Unit
                    is NoteEvent.CommentCountChanged -> Unit
                }
            }
        }
    }

    /// 点赞、取消点赞
    fun toggleLike(item: FeedItem, currentLiked: Boolean, currentCount: Int) {
        val nextLiked = !currentLiked
        val nextCount = if (currentLiked) currentCount - 1 else currentCount + 1
        _likeState.update {
            it.copy(
                likeStates = it.likeStates + (item.id to nextLiked),
                likeCounts = it.likeCounts + (item.id to nextCount),
            )
        }
        viewModelScope.launch {
            val result = if (currentLiked) noteRepository.unlikeNote(item.id)
                          else noteRepository.likeNote(item.id)
            result.onSuccess {
                eventBus.emit(NoteEvent.LikeChanged(item.id, nextLiked, nextCount))
            }
            result.onFailure {
                _likeState.update {
                    it.copy(
                        likeStates = it.likeStates + (item.id to currentLiked),
                        likeCounts = it.likeCounts + (item.id to currentCount),
                    )
                }
            }
        }
    }
}
