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
 * 个人主页内容 ViewModel，
 * 统一管理笔记、点赞、收藏三个 tab 的列表数据和操作
 */
@HiltViewModel
class ProfileNotesViewModel @Inject constructor(
    private val noteRepository: NoteRepository,
    private val userRepository: UserRepository,
    private val eventBus: NoteEventBus,
) : ViewModel() {

    private val _notesState = MutableStateFlow(ProfileNotesUiState(isLoading = true))
    val notesState: StateFlow<ProfileNotesUiState> = _notesState.asStateFlow()

    private val _likesState = MutableStateFlow(ProfileNotesUiState())
    val likesState: StateFlow<ProfileNotesUiState> = _likesState.asStateFlow()

    private val _favoritesState = MutableStateFlow(ProfileNotesUiState())
    val favoritesState: StateFlow<ProfileNotesUiState> = _favoritesState.asStateFlow()

    private var notesCursor: Long? = null
    private var likesCursor: Long? = null
    private var favoritesCursor: Long? = null
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

    /// 观察同步详情页的点赞变更，三个列表同步
    private fun observeNoteEvents() {
        viewModelScope.launch {
            eventBus.events.collect { event ->
                when (event) {
                    is NoteEvent.LikeChanged -> {
                        val sync: (ProfileNotesUiState) -> ProfileNotesUiState = { state ->
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
                        _notesState.update(sync)
                        _likesState.update(sync)
                        _favoritesState.update(sync)
                    }
                    is NoteEvent.FavoriteChanged -> Unit
                }
            }
        }
    }

    /**
     * 笔记 tab
     */

    private fun loadNotes() {
        val uid = userId ?: return
        viewModelScope.launch {
            _notesState.update { it.copy(isLoading = true, error = null, items = emptyList(), hasMore = true) }
            notesCursor = null
            noteRepository.getUserNotes(userId = uid, cursor = null).fold(
                onSuccess = { response ->
                    notesCursor = response.nextCursor
                    _notesState.update {
                        it.copy(
                            isLoading = false,
                            items = response.items,
                            likedIds = response.items.filter { item -> item.liked }.map { item -> item.id }.toSet(),
                            hasMore = response.hasMore,
                        )
                    }
                },
                onFailure = { e ->
                    _notesState.update { it.copy(isLoading = false, error = e.message) }
                },
            )
        }
    }

    fun loadMoreNotes() {
        val uid = userId ?: return
        val state = _notesState.value
        if (state.isLoadingMore || !state.hasMore || state.isLoading) return
        viewModelScope.launch {
            _notesState.update { it.copy(isLoadingMore = true) }
            noteRepository.getUserNotes(userId = uid, cursor = notesCursor).fold(
                onSuccess = { response ->
                    notesCursor = response.nextCursor
                    _notesState.update {
                        it.copy(
                            isLoadingMore = false,
                            items = it.items + response.items,
                            likedIds = it.likedIds + response.items.filter { item -> item.liked }.map { item -> item.id }.toSet(),
                            hasMore = response.hasMore,
                        )
                    }
                },
                onFailure = { e ->
                    _notesState.update { it.copy(isLoadingMore = false, error = e.message) }
                },
            )
        }
    }

    /**
     * 点赞 tab
     */

    /// 点赞 tab 懒加载
    fun loadLikesLazy() {
        val uid = userId ?: return
        if (_likesState.value.items.isNotEmpty() || _likesState.value.isLoading) return
        viewModelScope.launch {
            _likesState.update { it.copy(isLoading = true, error = null, items = emptyList(), hasMore = true) }
            likesCursor = null
            noteRepository.getUserLikes(userId = uid, cursor = null).fold(
                onSuccess = { response ->
                    likesCursor = response.nextCursor
                    _likesState.update {
                        it.copy(
                            isLoading = false,
                            items = response.items,
                            likedIds = response.items.filter { item -> item.liked }.map { item -> item.id }.toSet(),
                            hasMore = response.hasMore,
                        )
                    }
                },
                onFailure = { e ->
                    _likesState.update { it.copy(isLoading = false, error = e.message) }
                },
            )
        }
    }

    fun loadMoreLikes() {
        val uid = userId ?: return
        val state = _likesState.value
        if (state.isLoadingMore || !state.hasMore || state.isLoading) return
        viewModelScope.launch {
            _likesState.update { it.copy(isLoadingMore = true) }
            noteRepository.getUserLikes(userId = uid, cursor = likesCursor).fold(
                onSuccess = { response ->
                    likesCursor = response.nextCursor
                    _likesState.update {
                        it.copy(
                            isLoadingMore = false,
                            items = it.items + response.items,
                            likedIds = it.likedIds + response.items.filter { item -> item.liked }.map { item -> item.id }.toSet(),
                            hasMore = response.hasMore,
                        )
                    }
                },
                onFailure = { e ->
                    _likesState.update { it.copy(isLoadingMore = false, error = e.message) }
                },
            )
        }
    }

    /**
     * 收藏 tab
     */

    /// 懒加载收藏 tab
    fun loadFavoritesLazy() {
        val uid = userId ?: return
        if (_favoritesState.value.items.isNotEmpty() || _favoritesState.value.isLoading) return
        viewModelScope.launch {
            _favoritesState.update { it.copy(isLoading = true, error = null, items = emptyList(), hasMore = true) }
            favoritesCursor = null
            noteRepository.getUserFavorites(userId = uid, cursor = null).fold(
                onSuccess = { response ->
                    favoritesCursor = response.nextCursor
                    _favoritesState.update {
                        it.copy(
                            isLoading = false,
                            items = response.items,
                            likedIds = response.items.filter { item -> item.liked }.map { item -> item.id }.toSet(),
                            hasMore = response.hasMore,
                        )
                    }
                },
                onFailure = { e ->
                    _favoritesState.update { it.copy(isLoading = false, error = e.message) }
                },
            )
        }
    }

    fun loadMoreFavorites() {
        val uid = userId ?: return
        val state = _favoritesState.value
        if (state.isLoadingMore || !state.hasMore || state.isLoading) return
        viewModelScope.launch {
            _favoritesState.update { it.copy(isLoadingMore = true) }
            noteRepository.getUserFavorites(userId = uid, cursor = favoritesCursor).fold(
                onSuccess = { response ->
                    favoritesCursor = response.nextCursor
                    _favoritesState.update {
                        it.copy(
                            isLoadingMore = false,
                            items = it.items + response.items,
                            likedIds = it.likedIds + response.items.filter { item -> item.liked }.map { item -> item.id }.toSet(),
                            hasMore = response.hasMore,
                        )
                    }
                },
                onFailure = { e ->
                    _favoritesState.update { it.copy(isLoadingMore = false, error = e.message) }
                },
            )
        }
    }


    /// 点赞/取消点赞，三个列表同步
    fun toggleLike(noteId: Long) {
        val liked = noteId in _notesState.value.likedIds ||
                    noteId in _likesState.value.likedIds ||
                    noteId in _favoritesState.value.likedIds
        val delta = if (liked) -1 else 1
        /// 乐观更新
        val apply: (ProfileNotesUiState) -> ProfileNotesUiState = { state ->
            state.copy(
                likedIds = if (liked) state.likedIds - noteId else state.likedIds + noteId,
                items = state.items.map { item ->
                    if (item.id == noteId) item.copy(likeCount = item.likeCount + delta) else item
                },
            )
        }
        _notesState.update(apply)
        _likesState.update(apply)
        _favoritesState.update(apply)
        viewModelScope.launch {
            val result = if (liked) noteRepository.unlikeNote(noteId) else noteRepository.likeNote(noteId)
            result.onFailure {
                /// 请求失败时回滚
                val revert: (ProfileNotesUiState) -> ProfileNotesUiState = { state ->
                    state.copy(
                        likedIds = if (liked) state.likedIds + noteId else state.likedIds - noteId,
                        items = state.items.map { item ->
                            if (item.id == noteId) item.copy(likeCount = item.likeCount - delta) else item
                        },
                    )
                }
                _notesState.update(revert)
                _likesState.update(revert)
                _favoritesState.update(revert)
            }
        }
    }
}
