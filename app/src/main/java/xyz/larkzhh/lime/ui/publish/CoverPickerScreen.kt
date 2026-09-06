package xyz.larkzhh.lime.ui.publish

import android.view.TextureView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.compose.animation.core.animate
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.SeekParameters
import androidx.navigation.NavHostController
import coil3.compose.rememberAsyncImagePainter
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import xyz.larkzhh.lime.R
import xyz.larkzhh.lime.ui.publish.components.AlbumSquare
import xyz.larkzhh.lime.ui.publish.components.FrameScrubTrack
import xyz.larkzhh.lime.ui.publish.viewmodel.CropTransform
import xyz.larkzhh.lime.ui.publish.viewmodel.VideoPublishViewModel
import xyz.larkzhh.lime.ui.theme.LimePrimary

/// 预览层最大放大倍数
private const val MAX_COVER_SCALE = 8f

/// 越界最大溢出
private const val RUBBER_LIMIT = 0.05f

@androidx.annotation.OptIn(UnstableApi::class)
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
    val transform = publishState.editingTransform

    // 裁剪框宽高比
    val isLandscape = publishState.videoHeight > 0 && publishState.videoWidth > publishState.videoHeight
    val cropRatio = if (isLandscape) 4f / 3f else 3f / 4f

    // 视频画面真实宽高比
    val videoRatio = if (publishState.videoHeight > 0) {
        publishState.videoWidth.toFloat() / publishState.videoHeight
    } else cropRatio

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

    LaunchedEffect(frameMs, isAlbum, exoPlayer) {
        if (!isAlbum) exoPlayer?.seekTo(frameMs)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding()
    ) {
        // 顶栏
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black)
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.cover_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                modifier = Modifier.padding(start = 12.dp),
            )
        }

        // 预览裁剪区
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clipToBounds()
                .background(Color.Black),
            contentAlignment = Alignment.Center,
        ) {
            if (isAlbum && albumUri != null) {
                AlbumCropOverlay(
                    uri = albumUri,
                    cropRatio = cropRatio,
                    transform = transform,
                    onTransform = viewModel::setCoverTransform,
                )
            } else if (exoPlayer != null) {
                CoverCropOverlay(
                    mediaRatio = videoRatio,
                    cropRatio = cropRatio,
                    transform = transform,
                    onTransform = viewModel::setCoverTransform,
                ) { m ->
                    AndroidView(
                        factory = { ctx ->
                            TextureView(ctx).also { exoPlayer.setVideoTextureView(it) }
                        },
                        modifier = m,
                    )
                }
            }
        }

        // 相册与截帧滑轨
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .background(Color.Black)
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
                .background(Color.Black)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White.copy(alpha = 0.08f))
                    .clickable {
                        navController.popBackStack()
                    }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(stringResource(R.string.cover_exit), color = Color.White, fontWeight = FontWeight.SemiBold)
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
                Text(stringResource(R.string.cover_done), color = Color.White, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

/// 相册封面裁剪层
@Composable
private fun AlbumCropOverlay(
    uri: android.net.Uri,
    cropRatio: Float,
    transform: CropTransform,
    onTransform: (CropTransform) -> Unit,
) {
    val painter = rememberAsyncImagePainter(model = uri)
    val size = painter.intrinsicSize// 获取图片的原始物理尺寸
    val ratio = if (size.width > 0f && size.height > 0f) size.width / size.height else cropRatio
    CoverCropOverlay(
        mediaRatio = ratio,
        cropRatio = cropRatio,
        transform = transform,
        onTransform = onTransform,
    ) { m ->
        Image(
            painter = painter,
            contentDescription = stringResource(R.string.cover_preview),
            contentScale = ContentScale.Crop,
            modifier = m,
        )
    }
}

/// 封面裁剪预览层
@Composable
private fun CoverCropOverlay(
    mediaRatio: Float,
    cropRatio: Float,
    transform: CropTransform,
    onTransform: (CropTransform) -> Unit,
    media: @Composable (Modifier) -> Unit,
) {
    val density = LocalDensity.current
    // 手势更新本地变换
    var live by remember { mutableStateOf(transform) }
    LaunchedEffect(transform) { live = transform }
    val scope = rememberCoroutineScope()

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val contW = with(density) { maxWidth.toPx() }
        val contH = with(density) { maxHeight.toPx() }
        // 裁剪框
        val boxW = contW
        val boxH = boxW / cropRatio
        val boxLeft = 0f
        val boxTop = (contH - boxH) / 2f
        val strokePx = with(density) { 1.5.dp.toPx() }// 裁剪框边框宽度
        val fillWidth = mediaRatio <= cropRatio
        val baseW = if (fillWidth) contW else boxH * mediaRatio// 图比框窄，宽度填满
        val baseH = if (fillWidth) contW / mediaRatio else boxH// 图比框宽，高度填满

        val s = live.scale
        // 平移量
        val tx = (0.5f - live.focusX) * baseW * s
        val ty = (0.5f - live.focusY) * baseH * s

        // 媒体层
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .requiredSize(with(density) { baseW.toDp() }, with(density) { baseH.toDp() })
                .graphicsLayer(
                    scaleX = s,
                    scaleY = s,
                    translationX = tx,
                    translationY = ty,
                    transformOrigin = TransformOrigin(0.5f, 0.5f),
                ),
        ) {
            media(Modifier.fillMaxSize())
        }

        // 蒙版与白框
        Canvas(modifier = Modifier.fillMaxSize()) {
            val scrim = Color.Black.copy(alpha = 0.55f)
            drawRect(scrim, topLeft = Offset(0f, 0f), size = Size(contW, boxTop))
            drawRect(scrim, topLeft = Offset(0f, boxTop + boxH), size = Size(contW, contH - boxTop - boxH))
            drawRect(scrim, topLeft = Offset(0f, boxTop), size = Size(boxLeft, boxH))
            drawRect(scrim, topLeft = Offset(boxLeft + boxW, boxTop), size = Size(contW - boxLeft - boxW, boxH))
            drawRect(
                color = Color.White,
                topLeft = Offset(boxLeft, boxTop),
                size = Size(boxW, boxH),
                style = Stroke(width = strokePx),
            )
        }

        // 双指提示
        Text(
            text = stringResource(R.string.cover_pinch_hint),
            color = Color.White.copy(alpha = 0.85f),
            fontSize = 13.sp,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 16.dp),
        )

        // 平移、缩放手势
        var snapJob by remember { mutableStateOf<Job?>(null) }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(mediaRatio, cropRatio) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
                        snapJob?.cancel()// 有新触摸，打断上次回弹
                        do {
                            val event = awaitPointerEvent()
                            val zoom = event.calculateZoom()// 计算双指捏合的缩放因子
                            val pan = event.calculatePan()// 计算单指拖动的像素偏移量
                            if (zoom != 1f || pan != Offset.Zero) {
                                val cur = live
                                val scale = (cur.scale * zoom).coerceIn(1f, MAX_COVER_SCALE)
                                // 屏幕位移，焦点位移
                                val dFocusX = -pan.x / (baseW * scale)
                                val dFocusY = -pan.y / (baseH * scale)
                                // 裁剪框占图比例
                                val hx = boxW / (baseW * scale) / 2f
                                val hy = boxH / (baseH * scale) / 2f
                                val fx = rubberBand(cur.focusX + dFocusX, hx, 1f - hx)
                                val fy = rubberBand(cur.focusY + dFocusY, hy, 1f - hy)
                                live = CropTransform(scale = scale, focusX = fx, focusY = fy)
                                event.changes.forEach { if (it.positionChanged()) it.consume() }
                            }
                        } while (event.changes.any { it.pressed })
                        // 松手回弹
                        val end = live
                        val hx = boxW / (baseW * end.scale) / 2f
                        val hy = boxH / (baseH * end.scale) / 2f
                        val targetX = end.focusX.coerceIn(hx, 1f - hx)
                        val targetY = end.focusY.coerceIn(hy, 1f - hy)
                        if (targetX != end.focusX || targetY != end.focusY) {
                            snapJob = scope.launch {
                                animate(initialValue = 0f, targetValue = 1f) { t, _ ->
                                    live = end.copy(
                                        focusX = lerp(end.focusX, targetX, t),
                                        focusY = lerp(end.focusY, targetY, t),
                                    )
                                }
                                onTransform(live)
                            }
                        } else {
                            onTransform(live)
                        }
                    }
                }
        )
    }
}
/// 超出边界阻尼
private fun rubberBand(value: Float, min: Float, max: Float, limit: Float = RUBBER_LIMIT): Float = when {
    min > max -> (min + max) / 2f
    value < min -> min - rubberDelta(min - value, limit)
    value > max -> max + rubberDelta(value - max, limit)
    else -> value
}

/// 阻尼位移
private fun rubberDelta(over: Float, limit: Float): Float =
    limit * (1f - 1f / (over / limit + 1f))