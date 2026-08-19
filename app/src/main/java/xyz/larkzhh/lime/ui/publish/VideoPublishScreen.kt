package xyz.larkzhh.lime.ui.publish

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavHostController
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.video.videoFrameMillis
import xyz.larkzhh.lime.navigation.Screen
import xyz.larkzhh.lime.ui.publish.components.NotePublishScaffold
import xyz.larkzhh.lime.ui.publish.components.PickerVideoPreview
import xyz.larkzhh.lime.ui.publish.viewmodel.CoverSource
import xyz.larkzhh.lime.ui.publish.viewmodel.DEFAULT_COVER_FRAME_MS
import xyz.larkzhh.lime.ui.publish.viewmodel.VideoPublishViewModel

@Composable
fun VideoPublishScreen(
    navController: NavHostController,
    viewModel: VideoPublishViewModel,
) {
    val pickerState by viewModel.pickerState.collectAsState()
    val publishState by viewModel.publishState.collectAsState()
    val context = LocalContext.current

    val videoUri = pickerState.selectedVideo?.uri
    val hasCover = publishState.cover !is CoverSource.None

    // 视频预览
    var showPreview by remember { mutableStateOf(false) }

    // 视频预览尺寸，高度固定
    val previewHeight = 120.dp
    val isLandscape = publishState.videoHeight > 0 && publishState.videoWidth > publishState.videoHeight
    val ratio = if (isLandscape) 4f / 3f else 3f / 4f
    val previewWidth = previewHeight * ratio

    NotePublishScaffold(
        navController = navController,
        topBarTitle = "发布视频笔记",
        title = publishState.title,
        content = publishState.content,
        onTitleChange = viewModel::onTitleChange,
        onContentChange = viewModel::onContentChange,
        isPublishing = publishState.isPublishing,
        error = publishState.error,
        progressText = publishState.uploadPhase?.let { "正在$it..." },
        isSuccess = publishState.isSuccess,
        isDraftSuccess = publishState.isDraftSuccess,
        onClearSuccess = viewModel::clearSuccess,
        onClearDraftSuccess = viewModel::clearDraftSuccess,
        onSaveDraft = viewModel::saveDraft,
        onPublish = viewModel::publish,
    ) {
        // 顶部封面预览
        Box(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .height(previewHeight)
                .width(previewWidth)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
                .clickable(enabled = videoUri != null) { showPreview = true },
        ) {
            // 封面来源
            val coverModel: Any? = when (val cover = publishState.cover) {
                is CoverSource.Album -> cover.croppedUri ?: cover.uri
                is CoverSource.Frame -> cover.croppedUri ?: videoUri?.let {
                    ImageRequest.Builder(context).data(it).videoFrameMillis(cover.timeMs).build()
                }

                CoverSource.None -> videoUri?.let {
                    ImageRequest.Builder(context).data(it).videoFrameMillis(DEFAULT_COVER_FRAME_MS)
                        .build()
                }
            }
            if (coverModel != null) {
                AsyncImage(
                    model = coverModel,
                    contentDescription = "封面",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            // 播放角标
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp),
                )
            }
            // 选封面、更改封面
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.45f))
                    .clickable { navController.navigate(Screen.CoverPicker.route) }
                    .padding(vertical = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (hasCover) "更改封面" else "选封面",
                    color = Color.White,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }

    // 视频全屏预览
    val previewVideo = pickerState.selectedVideo
    if (showPreview && previewVideo != null) {
        Dialog(
            onDismissRequest = { showPreview = false },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false,
            ),
        ) {
            PickerVideoPreview(
                videos = listOf(previewVideo),
                initialIndex = 0,
                selectedId = null,
                onToggle = null,
                onDismiss = { showPreview = false },
            )
        }
    }
}
