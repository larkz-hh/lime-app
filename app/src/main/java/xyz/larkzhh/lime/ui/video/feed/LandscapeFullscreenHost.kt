package xyz.larkzhh.lime.ui.video.feed

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.util.UnstableApi
import kotlinx.coroutines.delay
import xyz.larkzhh.lime.ui.video.components.ScrubBar
import xyz.larkzhh.lime.ui.video.components.formatTime
import xyz.larkzhh.lime.ui.video.player.VideoPage
import xyz.larkzhh.lime.ui.video.player.VideoPlayerManager
import xyz.larkzhh.lime.util.LockLandscapeImmersive
import kotlin.time.Duration.Companion.milliseconds

/// 横屏全屏
@UnstableApi
@Composable
fun LandscapeFullscreenHost(
    viewModel: VideoFeedViewModel,
    playerManager: VideoPlayerManager,
    onExit: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()

    // 锁横屏沉浸式
    LockLandscapeImmersive(active = true)

    val items = uiState.landscapeItems
    val lastIndex = (items.size - 1).coerceAtLeast(0)
    val pagerState = rememberPagerState(
        initialPage = uiState.landscapeIndex.coerceIn(0, lastIndex),
    ) { items.size }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.collect { viewModel.onLandscapePageSettled(it) }
    }

    BackHandler { onExit() } // 退出全屏

    var scrubbing by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center,
    ) {
        if (items.isEmpty()) {
            return@Box
        }

        VerticalPager(
            state = pagerState,
            userScrollEnabled = !scrubbing,
            beyondViewportPageCount = 1,
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
        ) { page ->
            val item = items[page]
            val isActive = page == pagerState.settledPage
            var userPaused by remember(page) { mutableStateOf(false) }

            VideoPage(
                noteId = item.id,
                playUrl = item.video.playUrl,
                width = item.video.width,
                height = item.video.height,
                isActive = isActive,
                userPaused = userPaused,
                playerManager = playerManager,
                onTogglePlay = { userPaused = !userPaused },
            ) { player ->
                if (isActive) {
                    // 播放进度轮询
                    var positionMs by remember { mutableLongStateOf(0L) }
                    var durationMs by remember { mutableLongStateOf(item.video.durationMs.coerceAtLeast(1L)) }
                    LaunchedEffect(player) {
                        while (true) {
                            player?.let {
                                positionMs = it.currentPosition
                                val d = it.duration
                                if (d > 0) durationMs = d
                            }
                            delay(200.milliseconds)
                        }
                    }
                    val fraction = if (durationMs > 0) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f

                    // 左上返回
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "退出全屏",
                        tint = Color.White,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .displayCutoutPadding()
                            .padding(12.dp)
                            .size(40.dp)
                            .clip(CircleShape)
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() },
                                onClick = { onExit() },
                            )
                            .padding(8.dp),
                    )

                    // 底部进度条
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .displayCutoutPadding()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                    ) {
                        Text(
                            text = "${formatTime(positionMs)} / ${formatTime(durationMs)}",
                            color = Color.White,
                            fontSize = 12.sp,
                        )
                        ScrubBar(
                            fraction = fraction,
                            onDragStart = { scrubbing = true },
                            onSeek = { v -> player?.seekTo((v * durationMs).toLong()) },
                            onDragEnd = { v ->
                                player?.seekTo((v * durationMs).toLong())
                                scrubbing = false
                            },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}
