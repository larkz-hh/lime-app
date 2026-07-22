package xyz.larkzhh.lime.ui.publish.viewmodel

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import xyz.larkzhh.lime.domain.repository.NoteRepository
import javax.inject.Inject

data class LocalImage(val id: Long, val uri: Uri)

/// 相册选择页 UI 状态
data class PhotoPickerUiState(
    val images: List<LocalImage> = emptyList(),
    val selectedUris: List<Uri> = emptyList(),
    val isLoading: Boolean = false,
)

/// 笔记发布页 UI 状态
data class PublishUiState(
    val selectedUris: List<Uri> = emptyList(),
    val title: String = "",
    val content: String = "",
    val isPublishing: Boolean = false,
    val publishProgress: Int = 0,  // 已上传图片数
    val error: String? = null,
    val isSuccess: Boolean = false,
)

/**
 * 发布页与编辑页共享的 ViewModel。
 * 负责负责管理相册选择状态、图片列表查询以及笔记发布逻辑。
 */
@HiltViewModel
class PublishViewModel @Inject constructor(
    private val noteRepository: NoteRepository,
    @param:ApplicationContext private val context: Context,
) : ViewModel() {

    private val _pickerState = MutableStateFlow(PhotoPickerUiState())
    val pickerState: StateFlow<PhotoPickerUiState> = _pickerState.asStateFlow()

    private val _publishState = MutableStateFlow(PublishUiState())
    val publishState: StateFlow<PublishUiState> = _publishState.asStateFlow()

    /// 加载设备图片列表
    fun loadDeviceImages() {
        viewModelScope.launch {
            _pickerState.update { it.copy(isLoading = true) }
            val images = queryImages()
            _pickerState.update { it.copy(images = images, isLoading = false) }
        }
    }

    ///
    private suspend fun queryImages(): List<LocalImage> = withContext(Dispatchers.IO) {
        val result = mutableListOf<LocalImage>()
        val projection = arrayOf(MediaStore.Images.Media._ID)
        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"
        context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection, null, null, sortOrder
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)// 获取相应列索引
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val uri = ContentUris.withAppendedId(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id
                )
                result.add(LocalImage(id = id, uri = uri))
            }
        }
        result
    }

    /// 切换图片选中状态（至多9张）
    fun toggleImageSelection(uri: Uri) {
        val current = _pickerState.value.selectedUris
        val newList = if (current.contains(uri)) {
            current - uri
        } else if (current.size < 9) {
            current + uri
        } else {
            current
        }
        _pickerState.update { it.copy(selectedUris = newList) }
    }

    /// 确认选择，将相册选中的图片同步到发布页的状态
    fun confirmSelection() {
        val uris = _pickerState.value.selectedUris
        _publishState.update { it.copy(selectedUris = uris) }
    }

    /// 从发布页返回选择器追加图片时，先把当前已选图片同步回选择器
    fun preparePickerForAddMore() {
        _pickerState.update { it.copy(selectedUris = _publishState.value.selectedUris) }
    }

    /// 移除发布页中某张图片
    fun removeImage(uri: Uri) {
        _publishState.update { it.copy(selectedUris = it.selectedUris - uri) }
    }

    /// 改变笔记标题或内容
    fun onTitleChange(value: String) = _publishState.update { it.copy(title = value) }
    fun onContentChange(value: String) = _publishState.update { it.copy(content = value) }

    /// 发布笔记
    fun publish() {
        val state = _publishState.value
        if (state.selectedUris.isEmpty()) {
            _publishState.update { it.copy(error = "请至少添加一张图片") }
            return
        }
        if (state.title.isBlank() && state.content.isBlank()) {
            _publishState.update { it.copy(error = "标题和正文至少填写一项") }
            return
        }
        viewModelScope.launch {
            _publishState.update { it.copy(isPublishing = true, error = null, publishProgress = 0) }
            try {
                val uploadedUrls = mutableListOf<String>()// 已上传图片
                state.selectedUris.forEachIndexed { index, uri ->
                    val url = noteRepository.uploadImage(uri).getOrThrow()// 逐张上传图片，获取服务端返回的url
                    uploadedUrls.add(url)
                    _publishState.update { it.copy(publishProgress = index + 1) }
                }
                noteRepository.publishNote(
                    title = state.title.ifBlank { null },
                    content = state.content.ifBlank { null },
                    imageUrls = uploadedUrls,
                ).getOrThrow()
                _publishState.update { it.copy(isPublishing = false, isSuccess = true) }
            } catch (e: Exception) {
                _publishState.update {
                    it.copy(isPublishing = false, error = e.message ?: "发布失败，请重试")
                }
            }
        }
    }

    fun clearSuccess() = _publishState.update { it.copy(isSuccess = false) }
}
