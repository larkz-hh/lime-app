package xyz.larkzhh.lime.ui.publish

import android.view.SurfaceView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.SeekParameters
import androidx.navigation.NavHostController
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.video.videoFrameMillis
import kotlin.math.roundToLong
import xyz.larkzhh.lime.ui.publish.viewmodel.VideoPublishViewModel
import xyz.larkzhh.lime.ui.theme.LimePrimary

/// 滑轨缩略图数量
private const val THUMB_COUNT = 8

@OptIn(UnstableApi::class)
@Composable
fun CoverPickerScreen(
    navController: NavHostController,
    viewModel: VideoPublishViewModel,
) {
    val pickerState by viewModel.pickerState.collectAsState()
    val publishState by viewModel.publishState.collectAsState()
    val context = LocalContext.current

    val video = pickerState.selectedVideo
    val videoUri = video?.uri
    val durationMs = video?.durationMs ?: 0L

    // 进入使用提交封面、还原上次状态
    LaunchedEffect(Unit) { viewModel.beginCoverEdit() }

    // 相册选图
    val albumLauncher = rememberLauncherForActivityResult(PickVisualMedia()) { uri ->
        if (uri != null) viewModel.setCoverAlbum(uri)
    }

    val isAlbum = publishState.editingIsAlbum
    val albumUri = publishState.editingAlbumUri
    val frameMs = publishState.editingFrameMs

    // 预览宽高比
    val ratio = if (publishState.videoHeight > 0) {
        publishState.videoWidth.toFloat() / publishState.videoHeight
    } else 3f / 4f

    // 初始化 ExoPlayer
    val exoPlayer = remember(videoUri) {
        videoUri?.let {
            ExoPlayer.Builder(context).build().apply {
                setMediaItem(MediaItem.fromUri(it))
                setSeekParameters(SeekParameters.CLOSEST_SYNC) // 就近关键帧
                volume = 0f
                playWhenReady = false// 静态预览
                prepare()
                seekTo(frameMs)
            }
        }
    }
    DisposableEffect(exoPlayer) {
        onDispose { exoPlayer?.release() }
    }
    // 帧位置变化即 seek（含进入时的状态还原）；相册模式不动播放器
    LaunchedEffect(frameMs, isAlbum, exoPlayer) {
        if (!isAlbum) exoPlayer?.seekTo(frameMs)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        // 顶栏
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "选择封面",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 12.dp),
            )
        }

        // 预览区
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(16.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (isAlbum && albumUri != null) {
                AsyncImage(
                    model = albumUri,
                    contentDescription = "封面预览",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize(),
                )
            } else if (exoPlayer != null) {
                // 竖屏撑高、横屏撑宽
                val surfaceModifier = if (ratio < 1f) {
                    Modifier.fillMaxHeight().aspectRatio(ratio)
                } else {
                    Modifier.fillMaxWidth().aspectRatio(ratio)
                }
                AndroidView(
                    factory = { ctx ->
                        SurfaceView(ctx).also { exoPlayer.setVideoSurfaceView(it) }
                    },
                    modifier = surfaceModifier,
                )
            }
        }

        // 相册与截帧滑轨
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AlbumSquare(
                albumUri = albumUri,
                selected = isAlbum,
                onPick = { albumLauncher.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly)) },
                onUseAlbum = { viewModel.useEditingAlbum() },
                onClear = { viewModel.clearEditingCover() },
            )
            if (videoUri != null && durationMs > 0) {
                FrameScrubTrack(
                    videoUri = videoUri,
                    durationMs = durationMs,
                    currentMs = frameMs,
                    selected = !isAlbum,
                    onScrub = { viewModel.setCoverFrame(it) },
                    context = context,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        // 底部退出、完成
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                    .clickable {
                        navController.popBackStack()
                    }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("退出", fontWeight = FontWeight.SemiBold)
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(LimePrimary)
                    .clickable {
                        viewModel.commitCover()
                        navController.popBackStack()
                    }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("完成", color = Color.White, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

/// 相册入口
@Composable
private fun AlbumSquare(
    albumUri: android.net.Uri?,
    selected: Boolean,
    onPick: () -> Unit,
    onUseAlbum: () -> Unit,
    onClear: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
            .clickable {
                when {
                    albumUri == null -> onPick()// 打开相册
                    !selected -> onUseAlbum()// 切回相册图
                    else -> onPick()// 重新选一张
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        if (albumUri != null) {
            AsyncImage(
                model = albumUri,
                contentDescription = "相册封面",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            if (selected) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(LimePrimary.copy(alpha = 0.18f))
                )
            }
            // 清掉相册图回截帧
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(2.dp)
                    .size(16.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = 0.55f))
                    .clickable { onClear() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = "取消相册封面",
                    tint = Color.White,
                    modifier = Modifier.size(11.dp),
                )
            }
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Filled.Add,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .size(18.dp),
                )
                Text(
                    "相册",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/// 截帧滑轨
@Composable
private fun FrameScrubTrack(
    videoUri: android.net.Uri,
    durationMs: Long,
    currentMs: Long,
    selected: Boolean,
    onScrub: (Long) -> Unit,
    context: android.content.Context,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    var handleX by remember { mutableLongStateOf(currentMs) }
    LaunchedEffect(currentMs) { handleX = currentMs }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(6.dp)),
    ) {
        val trackWidthPx = with(density) { maxWidth.toPx() }

        // 缩略图轨道
        Row(modifier = Modifier.fillMaxSize()) {
            repeat(THUMB_COUNT) { i ->
                val t = durationMs * i / THUMB_COUNT
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(videoUri)
                        .videoFrameMillis(t)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize(),
                )
            }
        }

        // 未生效时压暗轨道
        if (!selected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.35f))
            )
        }

        // 拖动手柄
        val handleFraction = if (durationMs > 0) handleX.toFloat() / durationMs else 0f
        val handleWidthDp = 4.dp
        val handleOffsetDp = with(density) {
            ((trackWidthPx - handleWidthDp.toPx()) * handleFraction).toDp()
        }
        Box(
            modifier = Modifier
                .padding(start = handleOffsetDp)
                .width(handleWidthDp)
                .fillMaxSize()
                .background(Color.White)
        )

        //轨道的拖动、点按手势
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(durationMs, trackWidthPx) {
                    val update = { x: Float ->
                        val fraction = (x / trackWidthPx).coerceIn(0f, 1f)
                        val newMs = (fraction * durationMs).roundToLong()
                        handleX = newMs
                        onScrub(newMs)
                    }
                    detectDragGestures(
                        onDragStart = { offset -> update(offset.x) },
                        onDrag = { change, _ ->
                            change.consume()
                            update(change.position.x)
                        },
                    )
                }
        )
    }
}
