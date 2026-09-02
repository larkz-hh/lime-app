package xyz.larkzhh.lime.ui.ai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import xyz.larkzhh.lime.data.network.model.FeedItem
import xyz.larkzhh.lime.domain.repository.NoteRepository
import xyz.larkzhh.lime.domain.repository.UserRepository
import javax.inject.Inject

enum class NotePickerTab(val label: String) { FAVORITES("收藏"), LIKES("点赞"), PUBLISHED("发布") }

/**
 * 笔记选择器 ViewMode
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class NotePickerViewModel @Inject constructor(
    private val noteRepository: NoteRepository,
    private val userRepository: UserRepository,
) : ViewModel() {

    private val userIdFlow = MutableStateFlow<Long?>(null)

    val favoritesPager: Flow<PagingData<FeedItem>> = userIdFlow.filterNotNull().flatMapLatest { uid ->
        noteRepository.userFavoritesPager(uid, noteType = 1)
    }.cachedIn(viewModelScope)

    val likesPager: Flow<PagingData<FeedItem>> = userIdFlow.filterNotNull().flatMapLatest { uid ->
        noteRepository.userLikesPager(uid, noteType = 1)
    }.cachedIn(viewModelScope)

    val publishedPager: Flow<PagingData<FeedItem>> = userIdFlow.filterNotNull().flatMapLatest { uid ->
        noteRepository.userNotesPager(uid, noteType = 1)
    }.cachedIn(viewModelScope)

    init {
        viewModelScope.launch {
            userIdFlow.value = userRepository.userFlow.filterNotNull().first().id
        }
    }
}
