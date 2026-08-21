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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.util.UnstableApi
import kotlinx.coroutines.delay
import xyz.larkzhh.lime.R
import xyz.larkzhh.lime.ui.video.components.DanmakuInputSheet
import xyz.larkzhh.lime.ui.video.components.DanmakuOverlay
import xyz.larkzhh.lime.ui.video.components.ScrubBar
import xyz.larkzhh.lime.ui.video.components.formatTime
import xyz.larkzhh.lime.ui.video.player.VideoPage
import xyz.larkzhh.lime.ui.video.player.VideoPlayerManager
import xyz.larkzhh.lime.util.LockLandscapeImmersive
import xyz.larkzhh.lime.util.showToast
import kotlin.time.Duration.Companion.milliseconds

/// 横屏全屏
@UnstableApi
@Composable
fun LandscapeFullscreenHost(
    viewModel: VideoFeedViewModel,
    danmakuViewModel: DanmakuViewModel,
    playerManager: VideoPlayerManager,
    onExit: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    val danmakuUiState by danmakuViewModel.uiState.collectAsState()
    val context = LocalContext.current

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

    // 横屏切页加载弹幕
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.collect { page ->
            items.getOrNull(page)?.let { danmakuViewModel.setCurrentNote(it.id) }
        }
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
                onTogglePlay = {
                    if (!danmakuViewModel.dismissBubble()) userPaused = !userPaused
                },
                forcePaused = danmakuUiState.showInput,// 发弹幕时暂停当前视频
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

                    // 顶部弹幕区
                    DanmakuOverlay(
                        danmakuList = danmakuUiState.danmakuByNote[item.id].orEmpty(),
                        player = player,
                        enabled = danmakuUiState.enabled,
                        currentUserId = danmakuViewModel.currentUserId,
                        noteAuthorId = item.author.id,
                        pausedDanmakuId = danmakuUiState.pausedDanmakuId,
                        frozenMs = danmakuUiState.frozenMs,
                        onDanmakuClick = { id, nowMs -> danmakuViewModel.onDanmakuClick(id, nowMs) },
                        onDismissBubble = { danmakuViewModel.dismissBubble() },
                        onDelete = { danmakuViewModel.deleteDanmaku(item.id, it.id) },
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .fillMaxWidth()
                            .displayCutoutPadding()
                            .padding(top = 60.dp, start = 60.dp, end = 60.dp),
                    )

                    // 弹幕键
                    Icon(
                        painter = painterResource(R.drawable.ic_barrage),
                        contentDescription = "发弹幕",
                        tint = Color.White,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .displayCutoutPadding()
                            .padding(16.dp)
                            .size(28.dp)
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() },
                                onClick = { danmakuViewModel.openInput() },
                            ),
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

        // 弹幕输入框
        if (danmakuUiState.showInput) {
            val current = items.getOrNull(pagerState.currentPage.coerceIn(0, lastIndex))
            DanmakuInputSheet(
                color = danmakuUiState.color,
                onColorChange = danmakuViewModel::setColor,
                onToggleOff = {
                    danmakuViewModel.toggleEnabled()
                    danmakuViewModel.closeInput()
                    "弹幕已关闭".showToast(context)
                },
                onSend = { text ->
                    val item = current ?: return@DanmakuInputSheet
                    val pos = playerManager.currentPositionOf(item.id)
                    danmakuViewModel.sendDanmaku(item.id, text, pos)
                    danmakuViewModel.closeInput()
                },
                onDismiss = danmakuViewModel::closeInput,
            )
        }
    }
}
