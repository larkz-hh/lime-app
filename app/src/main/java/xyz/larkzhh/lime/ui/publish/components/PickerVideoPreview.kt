package xyz.larkzhh.lime.ui.publish.components

import android.view.TextureView
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.delay
import xyz.larkzhh.lime.R
import xyz.larkzhh.lime.ui.publish.viewmodel.LocalVideo
import xyz.larkzhh.lime.ui.video.components.ScrubBar
import xyz.larkzhh.lime.ui.video.components.formatTime
import kotlin.time.Duration.Companion.milliseconds

/// 视频全屏预览浮层
@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun PickerVideoPreview(
    videos: List<LocalVideo>,
    initialIndex: Int,
    selectedId: Long?,
    onToggle: ((LocalVideo) -> Unit)?,
    onDismiss: () -> Unit,
) {
    BackHandler(onBack = onDismiss)
    val pagerState = rememberPagerState(initialPage = initialIndex) { videos.size }
    // 拖动进度条时禁用翻页
    var scrubbing by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            beyondViewportPageCount = 0,
            userScrollEnabled = !scrubbing,
        ) { page ->
            if (page == pagerState.currentPage) {
                VideoPage(
                    video = videos[page],
                    onScrubbingChange = { scrubbing = it },
                )
            } else {
                Box(Modifier.fillMaxSize())
            }
        }

        // 顶栏
        val current = videos[pagerState.currentPage]
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.3f))
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 2.dp),
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.back),
                tint = Color.White,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .size(24.dp)
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                        onClick = onDismiss,
                    ),
            )
            if (onToggle != null && current.selectable) {
                SelectionCircle(
                    selected = selectedId == current.id,
                    label = null,
                    onClick = { onToggle(current) },
                    modifier = Modifier.align(Alignment.CenterEnd),
                )
            }
        }
    }
}

/// 单页视频
@androidx.annotation.OptIn(UnstableApi::class)
@Composable
private fun VideoPage(
    video: LocalVideo,
    onScrubbingChange: (Boolean) -> Unit,
) {
    val context = LocalContext.current
    val exoPlayer = remember(video.uri) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(video.uri))
            repeatMode = Player.REPEAT_MODE_ONE// 单曲循环
            prepare()
            playWhenReady = true
        }
    }
    DisposableEffect(exoPlayer) {
        onDispose { exoPlayer.release() }
    }

    var isPlaying by remember { mutableStateOf(true) }
    var positionMs by remember { mutableLongStateOf(0L) }
    var isDragging by remember { mutableStateOf(false) }
    var videoRatio by remember { mutableFloatStateOf(0f) }
    val durationMs = video.durationMs.coerceAtLeast(1L)

    // 监听真实画面宽高比，按原比例显示
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onVideoSizeChanged(videoSize: androidx.media3.common.VideoSize) {
                val w = videoSize.width * videoSize.pixelWidthHeightRatio
                val h = videoSize.height.toFloat()
                if (w > 0f && h > 0f) videoRatio = w / h
            }
        }
        exoPlayer.addListener(listener)
        onDispose { exoPlayer.removeListener(listener) }
    }

    // 轮询播放进度
    LaunchedEffect(exoPlayer) {
        while (true) {
            if (!isDragging) positionMs = exoPlayer.currentPosition
           delay(100.milliseconds)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
            ) {
                if (exoPlayer.isPlaying) {
                    exoPlayer.pause()
                    isPlaying = false
                } else {
                    exoPlayer.play()
                    isPlaying = true
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        AndroidView(
            factory = { ctx ->
                TextureView(ctx).also { exoPlayer.setVideoTextureView(it) }
            },
            modifier = if (videoRatio > 0f) {
                Modifier
                    .fillMaxSize()
                    .aspectRatio(videoRatio)
            } else {
                Modifier.fillMaxSize()
            },
        )

        // 暂停时播放图标
        if (!isPlaying) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.PlayArrow,
                    contentDescription = stringResource(R.string.pip_play),
                    tint = Color.White,
                    modifier = Modifier.size(40.dp),
                )
            }
        }

        // 底部进度条
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.3f))
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = formatTime(positionMs),
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
            )
            ScrubBar(
                fraction = (positionMs.toFloat() / durationMs).coerceIn(0f, 1f),
                onDragStart = {
                    isDragging = true
                    onScrubbingChange(true)
                },
                onSeek = { f ->
                    positionMs = (f * durationMs).toLong()
                },
                onDragEnd = { f ->
                    exoPlayer.seekTo((f * durationMs).toLong())
                    isDragging = false
                    onScrubbingChange(false)
                },
                modifier = Modifier.weight(1f),
            )
            Text(
                text = formatTime(durationMs),
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

