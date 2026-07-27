package xyz.larkzhh.lime.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import xyz.larkzhh.lime.data.network.model.NoteDetailData
import xyz.larkzhh.lime.domain.NoteEvent
import xyz.larkzhh.lime.domain.NoteEventBus
import xyz.larkzhh.lime.domain.repository.NoteRepository
import javax.inject.Inject


data class DetailUiState(
    val note: NoteDetailData? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
    val previewImageIndex: Int? = null, // null 不展示图片预览浮层
)

/**
 * 笔记详情页面相关 ViewModel
 * 负责管理页面的 UI 状态、获取笔记信息、点赞、收藏等业务逻辑。
 */
@HiltViewModel
class DetailViewModel @Inject constructor(
    private val noteRepository: NoteRepository,
    private val eventBus: NoteEventBus,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DetailUiState())
    val uiState = _uiState.asStateFlow()

    fun showImagePreview(index: Int) {
        _uiState.update { it.copy(previewImageIndex = index) }
    }

    fun hideImagePreview() {
        _uiState.update { it.copy(previewImageIndex = null) }
    }

    fun loadNote(noteId: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            noteRepository.getNoteDetail(noteId)
                .onSuccess { note ->
                    _uiState.update { it.copy(note = note, isLoading = false) }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                }
        }
    }

    fun toggleLike() {
        val note = _uiState.value.note ?: return
        viewModelScope.launch {
            _uiState.update { s ->
                s.copy(note = note.copy(
                    liked = !note.liked,
                    likeCount = if (note.liked) note.likeCount - 1 else note.likeCount + 1,
                ))
            }
            val result = if (note.liked) noteRepository.unlikeNote(note.id)
                         else noteRepository.likeNote(note.id)
            result.fold(
                onSuccess = {
                    val updated = _uiState.value.note ?: return@fold
                    eventBus.emit(NoteEvent.LikeChanged(updated.id, updated.liked, updated.likeCount))// 发送事件
                },
                onFailure = { _uiState.update { it.copy(note = note) } },
            )
        }
    }

    fun toggleFavorite() {
        val note = _uiState.value.note ?: return
        viewModelScope.launch {
            _uiState.update { s ->
                s.copy(note = note.copy(
                    favorited = !note.favorited,
                    favCount = if (note.favorited) note.favCount - 1 else note.favCount + 1,
                ))
            }
            val result = if (note.favorited) noteRepository.unfavoriteNote(note.id)
                         else noteRepository.favoriteNote(note.id)
            result.fold(
                onSuccess = {
                    val updated = _uiState.value.note ?: return@fold
                    eventBus.emit(NoteEvent.FavoriteChanged(updated.id, updated.favorited, updated.favCount))
                },
                onFailure = { _uiState.update { it.copy(note = note) } },
            )
        }
    }
}
