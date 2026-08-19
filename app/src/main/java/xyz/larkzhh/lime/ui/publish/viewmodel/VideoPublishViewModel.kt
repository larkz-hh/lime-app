package xyz.larkzhh.lime.ui.publish.viewmodel

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.video.videoFrameMillis
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
import xyz.larkzhh.lime.util.cropCoverToCache
import xyz.larkzhh.lime.util.extractFrameToCache
import xyz.larkzhh.lime.util.readVideoDimensions
import javax.inject.Inject

/// 视频限制常量
private const val MAX_VIDEO_SIZE = 200L * 1024 * 1024 // 200MB
private const val MAX_VIDEO_DURATION_MS = 10L * 60 * 1000 // 10 分钟
// 未选封面默认帧
const val DEFAULT_COVER_FRAME_MS = 100L

/// 设备本地视频
data class LocalVideo(
    val id: Long,
    val uri: Uri,
    val durationMs: Long,
    val sizeBytes: Long,
    val mimeType: String,
) {
    /// 仅允许mp4、不超过大小与时长限制
    val selectable: Boolean
        get() = mimeType == "video/mp4" &&
            sizeBytes <= MAX_VIDEO_SIZE &&
            durationMs <= MAX_VIDEO_DURATION_MS
}

/// 裁剪变换，缩放、焦点
data class CropTransform(
    val scale: Float = 1f,
    val focusX: Float = 0.5f,
    val focusY: Float = 0.5f,
)

/// 封面来源，未选、相册选图、视频截帧
sealed interface CoverSource {
    data object None : CoverSource
    data class Album(
        val uri: Uri,
        val transform: CropTransform = CropTransform(),
        val croppedUri: Uri? = null,
    ) : CoverSource
    data class Frame(
        val timeMs: Long,
        val transform: CropTransform = CropTransform(),
        val croppedUri: Uri? = null,
    ) : CoverSource
}

data class VideoPickerUiState(
    val videos: List<LocalVideo> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasMore: Boolean = true,
    val selectedVideo: LocalVideo? = null,
)

data class VideoPublishUiState(
    val videoWidth: Int = 0,
    val videoHeight: Int = 0,
    val cover: CoverSource = CoverSource.None,
    val editingAlbumUri: Uri? = null,
    val editingFrameMs: Long = DEFAULT_COVER_FRAME_MS,
    val editingIsAlbum: Boolean = false,
    val editingTransform: CropTransform = CropTransform(),
    val title: String = "",
    val content: String = "",
    val isPublishing: Boolean = false,
    val uploadPhase: String? = null,
    val error: String? = null,
    val isSuccess: Boolean = false,
    val isDraftSuccess: Boolean = false,
)

/**
 * 视频笔记发布流程的 ViewModel。
 * 管理视频发布与草稿、封面选择等
 */
@HiltViewModel
class VideoPublishViewModel @Inject constructor(
    private val noteRepository: NoteRepository,
    @param:ApplicationContext private val context: Context,
) : ViewModel() {

    private val _pickerState = MutableStateFlow(VideoPickerUiState())
    val pickerState: StateFlow<VideoPickerUiState> = _pickerState.asStateFlow()

    private val _publishState = MutableStateFlow(VideoPublishUiState())
    val publishState: StateFlow<VideoPublishUiState> = _publishState.asStateFlow()
    private val videoPageSize = 200

    /// 加载设备视频列表
    fun loadDeviceVideos() {
        viewModelScope.launch {
            _pickerState.update { it.copy(isLoading = true) }
            val videos = queryVideos(limit = videoPageSize, offset = 0)
            _pickerState.update {
                it.copy(videos = videos, isLoading = false, hasMore = videos.size >= videoPageSize)
            }
        }
    }

    /// 滚动到底加载更多视频
    fun loadMoreVideos() {
        val current = _pickerState.value
        if (!current.hasMore || current.isLoading || current.isLoadingMore) return
        viewModelScope.launch {
            _pickerState.update { it.copy(isLoadingMore = true) }
            val more = queryVideos(limit = videoPageSize, offset = current.videos.size)
            _pickerState.update {
                it.copy(
                    videos = it.videos + more,
                    isLoadingMore = false,
                    hasMore = more.size >= videoPageSize,
                )
            }
        }
    }

    /// 查询设备本地视频
    private suspend fun queryVideos(limit: Int, offset: Int): List<LocalVideo> = withContext(Dispatchers.IO) {
        val result = mutableListOf<LocalVideo>()
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.MIME_TYPE,
        )
        val args = Bundle().apply {
            putStringArray(
                ContentResolver.QUERY_ARG_SORT_COLUMNS,
                arrayOf(MediaStore.Video.Media.DATE_ADDED),
            )
            putInt(ContentResolver.QUERY_ARG_SORT_DIRECTION, ContentResolver.QUERY_SORT_DIRECTION_DESCENDING)
            putInt(ContentResolver.QUERY_ARG_LIMIT, limit)
            putInt(ContentResolver.QUERY_ARG_OFFSET, offset)
        }
        context.contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            projection, args, null
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val durCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
            val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.MIME_TYPE)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val uri = ContentUris.withAppendedId(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id
                )
                result.add(
                    LocalVideo(
                        id = id,
                        uri = uri,
                        durationMs = cursor.getLong(durCol),
                        sizeBytes = cursor.getLong(sizeCol),
                        mimeType = cursor.getString(mimeCol) ?: "",
                    )
                )
            }
        }
        result
    }

    /// 选中一个视频，同时读取其显示宽高
    fun selectVideo(video: LocalVideo) {
        // 换成不同视频时旧封面作废
        val changed = _pickerState.value.selectedVideo?.uri != video.uri
        _pickerState.update { it.copy(selectedVideo = video) }
        if (changed) {
            _publishState.update {
                it.copy(
                    cover = CoverSource.None,
                    editingAlbumUri = null,
                    editingFrameMs = DEFAULT_COVER_FRAME_MS,
                    editingIsAlbum = false,
                    editingTransform = CropTransform(),
                )
            }
        }
        viewModelScope.launch {
            val dim = readVideoDimensions(context, video.uri)
            _publishState.update { it.copy(videoWidth = dim.width, videoHeight = dim.height) }
        }
    }

    /// 进入封面页，还原上次状态
    fun beginCoverEdit() {
        _publishState.update {
            when (val c = it.cover) {
                is CoverSource.Album -> it.copy(
                    editingAlbumUri = c.uri,
                    editingFrameMs = DEFAULT_COVER_FRAME_MS,
                    editingIsAlbum = true,
                    editingTransform = c.transform,
                )
                is CoverSource.Frame -> it.copy(
                    editingAlbumUri = null,
                    editingFrameMs = c.timeMs,
                    editingIsAlbum = false,
                    editingTransform = c.transform,
                )
                CoverSource.None -> it.copy(
                    editingAlbumUri = null,
                    editingFrameMs = DEFAULT_COVER_FRAME_MS,
                    editingIsAlbum = false,
                    editingTransform = CropTransform(),
                )
            }
        }
    }

    /// 相册选图作为封面，换新图复位裁剪位置
    fun setCoverAlbum(uri: Uri) {
        _publishState.update {
            it.copy(
                editingAlbumUri = uri,
                editingIsAlbum = true,
                editingTransform = CropTransform(),
            )
        }
    }

    /// 滑轨截帧作为封面，切帧保留裁剪位置；从相册切回截帧复位
    fun setCoverFrame(timeMs: Long) {
        _publishState.update {
            it.copy(
                editingFrameMs = timeMs,
                editingIsAlbum = false,
                editingTransform = if (it.editingIsAlbum) CropTransform() else it.editingTransform,
            )
        }
    }

    /// 预览层拖动、缩放，更新裁剪变换
    fun setCoverTransform(transform: CropTransform) {
        _publishState.update { it.copy(editingTransform = transform) }
    }

    /// 取消相册选图，清掉相册图并回到截帧模式，复位裁剪位置
    fun clearEditingCover() {
        _publishState.update {
            it.copy(editingAlbumUri = null, editingIsAlbum = false, editingTransform = CropTransform())
        }
    }

    /// 点相册预览，切回已选相册图
    fun useEditingAlbum() {
        _publishState.update {
            if (it.editingAlbumUri != null) {
                it.copy(editingIsAlbum = true, editingTransform = CropTransform())
            } else it
        }
    }

    /// 提交编辑副本为正式封面，按裁剪变换切出封面发布页预览
    fun commitCover() {
        val state = _publishState.value
        val videoUri = _pickerState.value.selectedVideo?.uri
        val ratio = if (state.videoHeight > 0 && state.videoWidth > state.videoHeight) {
            4f / 3f
        } else 3f / 4f
        val transform = state.editingTransform

        viewModelScope.launch {
            val request = when {
                state.editingIsAlbum && state.editingAlbumUri != null ->
                    ImageRequest.Builder(context)
                        .data(state.editingAlbumUri)
                        .allowHardware(false)
                        .build()
                videoUri != null ->
                    ImageRequest.Builder(context)
                        .data(videoUri)
                        .videoFrameMillis(state.editingFrameMs)
                        .allowHardware(false)
                        .build()
                else -> null
            }
            val croppedUri = request?.let {
                cropCoverToCache(context, it, ratio, transform.scale, transform.focusX, transform.focusY)
            }
            _publishState.update {
                val committed = if (it.editingIsAlbum && it.editingAlbumUri != null) {
                    CoverSource.Album(it.editingAlbumUri, transform, croppedUri)
                } else {
                    CoverSource.Frame(it.editingFrameMs, transform, croppedUri)
                }
                it.copy(cover = committed)
            }
        }
    }

    fun onTitleChange(value: String) = _publishState.update { it.copy(title = value) }
    fun onContentChange(value: String) = _publishState.update { it.copy(content = value) }

    /// 提交视频笔记
    private fun submitVideo(status: Int) {
        val video = _pickerState.value.selectedVideo
        if (video == null) {
            _publishState.update { it.copy(error = "请先选择视频") }
            return
        }
        val state = _publishState.value
        viewModelScope.launch {
            _publishState.update { it.copy(isPublishing = true, error = null, uploadPhase = "上传视频") }
            try {
                val videoUrl = noteRepository.uploadVideo(video.uri).getOrThrow()

                _publishState.update { it.copy(uploadPhase = "上传封面") }
                val coverUrl = resolveCoverUrl(state.cover, video.uri)

                noteRepository.publishVideoNote(
                    title = state.title.ifBlank { null },
                    content = state.content.ifBlank { null },
                    videoUrl = videoUrl,
                    durationMs = video.durationMs,
                    width = state.videoWidth,
                    height = state.videoHeight,
                    coverUrl = coverUrl,
                    status = status,
                ).getOrThrow()

                val isDraft = status == 0
                _publishState.update {
                    it.copy(
                        isPublishing = false,
                        uploadPhase = null,
                        isSuccess = !isDraft,
                        isDraftSuccess = isDraft,
                    )
                }
            } catch (e: Exception) {
                val errorMsg = if (status == 0) "存草稿失败，请重试" else "发布失败，请重试"
                _publishState.update {
                    it.copy(isPublishing = false, uploadPhase = null, error = e.message ?: errorMsg)
                }
            }
        }
    }

    /// 解析封面上传
    private suspend fun resolveCoverUrl(cover: CoverSource, videoUri: Uri): String? {
        val coverUri: Uri? = when (cover) {
            is CoverSource.Album -> cover.croppedUri ?: cover.uri
            is CoverSource.Frame -> cover.croppedUri ?: extractFrameToCache(context, videoUri, cover.timeMs)
            CoverSource.None -> extractFrameToCache(context, videoUri, DEFAULT_COVER_FRAME_MS)
        }
        return coverUri?.let { noteRepository.uploadImage(it).getOrThrow() }
    }

    /// 发布视频笔记
    fun publish() = submitVideo(1)

    /// 存草稿
    fun saveDraft() = submitVideo(0)

    fun clearSuccess() = _publishState.update { it.copy(isSuccess = false) }
    fun clearDraftSuccess() = _publishState.update { it.copy(isDraftSuccess = false) }
}
