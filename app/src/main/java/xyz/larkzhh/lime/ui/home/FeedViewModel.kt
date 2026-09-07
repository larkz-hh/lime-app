package xyz.larkzhh.lime.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import xyz.larkzhh.lime.data.local.TokenStorage
import xyz.larkzhh.lime.data.network.model.FeedItem
import xyz.larkzhh.lime.domain.NoteEvent
import xyz.larkzhh.lime.domain.NoteEventBus
import xyz.larkzhh.lime.domain.repository.NoteRepository
import xyz.larkzhh.lime.util.system.NetworkMonitor
import javax.inject.Inject

data class FeedUiState(
    val likeStates: Map<Long, Boolean> = emptyMap(),
    val likeCounts: Map<Long, Int> = emptyMap(),
    val isOffline: Boolean = false,
)

@HiltViewModel
class FeedViewModel @Inject constructor(
    private val noteRepository: NoteRepository,
    private val eventBus: NoteEventBus,
    networkMonitor: NetworkMonitor,
    tokenStorage: TokenStorage,
) : ViewModel() {

    private val _uiState = MutableStateFlow(FeedUiState())
    val uiState: StateFlow<FeedUiState> = _uiState.asStateFlow()

    /// 当前登录用户 id
    val currentUserId: StateFlow<Long?> = tokenStorage.currentUserIdFlow

    /// 是否已登录
    val isLoggedIn: StateFlow<Boolean> = tokenStorage.isLoggedInFlow

    /// 发现页信息流
    val discoverFeed: Flow<PagingData<FeedItem>> =
        noteRepository.discoverFeedPager().cachedIn(viewModelScope)

    /// 关注页信息流
    val followingFeed: Flow<PagingData<FeedItem>> =
        noteRepository.followingFeedPager().cachedIn(viewModelScope)

    init {
        observeNoteEvents()
        observeNetwork(networkMonitor)
    }

    /// 同步其它页面的点赞变更
    private fun observeNoteEvents() {
        viewModelScope.launch {
            eventBus.events.collect { event ->
                when (event) {
                    is NoteEvent.LikeChanged -> _uiState.update {
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

    private fun observeNetwork(networkMonitor: NetworkMonitor) {
        viewModelScope.launch {
            networkMonitor.isOnline.collect { online ->
                _uiState.update { it.copy(isOffline = !online) }
            }
        }
    }

    // 点赞、取消点赞
    fun toggleLike(item: FeedItem, currentLiked: Boolean, currentCount: Int) {
        val nextLiked = !currentLiked
        val nextCount = if (currentLiked) currentCount - 1 else currentCount + 1
        _uiState.update {
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
                _uiState.update {
                    it.copy(
                        likeStates = it.likeStates + (item.id to currentLiked),
                        likeCounts = it.likeCounts + (item.id to currentCount),
                    )
                }
            }
        }
    }
}
