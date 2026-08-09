package xyz.larkzhh.lime.ui.detail.comment.viewmodel

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
import xyz.larkzhh.lime.ui.publish.viewmodel.LocalImage
import javax.inject.Inject

data class ImagePickerUiState(
    val images: List<LocalImage> = emptyList(),
    val selectedUris: List<Uri> = emptyList(),
    val isLoading: Boolean = false,
)

/**
 * 评论图片选择器页面的 ViewModel。
 *
 * 负责处图片选择页面加载、切换图片等业务逻辑。
 */
@HiltViewModel
class ImagePickerViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ImagePickerUiState())
    val uiState: StateFlow<ImagePickerUiState> = _uiState.asStateFlow()

    /// 加载设备相册中的图片
    fun load() {
        if (_uiState.value.images.isNotEmpty()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val images = queryImages()
            _uiState.update { it.copy(images = images, isLoading = false) }
        }
    }

    /// 切换指定 uri 图片的选中状态
    fun toggle(uri: Uri) {
        val current = _uiState.value.selectedUris
        val newList = if (current.contains(uri)) {
            current - uri
        } else if (current.size < 9) {
            current + uri
        } else {
            current
        }
        _uiState.update { it.copy(selectedUris = newList) }
    }

    /// 查询设备媒体库获取所有图片
    private suspend fun queryImages(): List<LocalImage> = withContext(Dispatchers.IO) {
        val result = mutableListOf<LocalImage>()
        val projection = arrayOf(MediaStore.Images.Media._ID)
        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"
        context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection, null, null, sortOrder
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
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
}
