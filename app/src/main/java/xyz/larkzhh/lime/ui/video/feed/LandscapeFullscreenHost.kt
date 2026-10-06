package xyz.larkzhh.lime.ui.video.feed

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.util.UnstableApi
import kotlinx.coroutines.delay
import xyz.larkzhh.lime.R
import xyz.larkzhh.lime.ui.components.FavoriteButton
import xyz.larkzhh.lime.ui.components.LikeButton
import xyz.larkzhh.lime.ui.video.components.DanmakuInputSheet
import xyz.larkzhh.lime.ui.video.components.DanmakuHost
import xyz.larkzhh.lime.ui.video.components.ScrubBar
import xyz.larkzhh.lime.ui.video.components.SpeedDrawer
import xyz.larkzhh.lime.ui.video.components.VerticalSlider
import xyz.larkzhh.lime.ui.video.components.formatSpeed
import xyz.larkzhh.lime.ui.video.components.formatTime
import xyz.larkzhh.lime.ui.video.player.VideoPage
import xyz.larkzhh.lime.ui.video.player.VideoPlayerManager
import xyz.larkzhh.lime.util.system.LockLandscapeImmersive
import xyz.larkzhh.lime.util.system.rememberBrightnessController
import xyz.larkzhh.lime.util.system.rememberVolumeController
import xyz.larkzhh.lime.util.showToast
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
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
    val isUnmetered by viewModel.isUnmetered.collectAsState()
    val context = LocalContext.current
    val danmakuOffToast = stringResource(R.string.video_danmaku_off)
    val danmakuOnToast = stringResource(R.string.video_danmaku_on)
    val speedBoostToast = stringResource(R.string.video_speed_boost)

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
    var showSpeedDrawer by remember { mutableStateOf(false) }// 速度面板
    var pressBoost by remember { mutableStateOf(false) }
    val volumeController = rememberVolumeController()
    val brightnessController = rememberBrightnessController()
    val scope = rememberCoroutineScope()

    // 退出全屏时恢复窗口亮度
    DisposableEffect(Unit) {
        onDispose { brightnessController.reset() }
    }

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
            beyondViewportPageCount = if (isUnmetered) 1 else 0,
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
        ) { page ->
            val item = items[page]
            val isActive = page == pagerState.settledPage
            val userPaused = item.id in uiState.pausedNoteIds
            var controlsVisible by remember(page) { mutableStateOf(true) }// 控制层显隐
            var adjustMode by remember(page) { mutableStateOf<String?>(null) }
            var brightness by remember(page) { mutableFloatStateOf(0f) }
            var volume by remember(page) { mutableFloatStateOf(0f) }

            // 播放中自动隐藏控制层
            LaunchedEffect(controlsVisible, userPaused, isActive, scrubbing) {
                if (isActive && controlsVisible && !userPaused && !scrubbing) {
                    delay(3000.milliseconds)
                    controlsVisible = false
                }
            }

            // 调节柱自动隐藏
            LaunchedEffect(adjustMode, brightness, volume) {
                if (adjustMode != null) {
                    delay(2000.milliseconds)
                    adjustMode = null
                }
            }

            VideoPage(
                noteId = item.id,
                playUrl = item.video.playUrl,
                width = item.video.width,
                height = item.video.height,
                isActive = isActive,
                userPaused = userPaused,
                playerManager = playerManager,
                onTogglePlay = {},// 全屏启用自定义手势
                title = item.title,
                forcePaused = danmakuUiState.showInput,// 发弹幕时暂停当前视频
                showPauseIcon = false,
                playbackSpeed = if (pressBoost) 2f else uiState.playbackSpeed,// 长按2倍速
                autoPlayNext = uiState.autoPlayNext,
                backgroundAudio = uiState.backgroundAudio,
                onPlaybackEnded = {
                    // 自动连播
                    val next = page + 1
                    if (next <= lastIndex) scope.launch { pagerState.animateScrollToPage(next) }
                },
                gestureModifier = Modifier
                    .pointerInput(item.id) {
                        val longPressMs = viewConfiguration.longPressTimeoutMillis
                        detectTapGestures(
                            // 单击，优先关气泡、调节柱，再切换控制层
                            onTap = {
                                when {
                                    danmakuViewModel.dismissBubble() -> {}
                                    adjustMode != null -> adjustMode = null
                                    else -> controlsVisible = !controlsVisible
                                }
                            },
                            // 双击，暂停播放
                            onDoubleTap = {
                                danmakuViewModel.dismissBubble()
                                viewModel.togglePaused(item.id)
                            },
                            // 长按标记
                            onLongPress = {},
                            onPress = {
                                val released = withTimeoutOrNull(longPressMs.milliseconds) { tryAwaitRelease() }
                                if (released == null) {
                                    pressBoost = true
                                    speedBoostToast.showToast(context)
                                    tryAwaitRelease()// 等待松手
                                    pressBoost = false
                                }
                            },
                        )
                    },
            ) { player ->
                if (isActive) {
                    // 播放进度轮询
                    var positionMs by remember { mutableLongStateOf(0L) }
                    var durationMs by remember { mutableLongStateOf(item.video.durationMs.coerceAtLeast(1L)) }
                    LaunchedEffect(player) {
                        while (true) {
                            player?.let {
                                positionMs = it.currentPosition
                                danmakuViewModel.onPlayheadMoved(item.id, it.currentPosition)
                                val d = it.duration
                                if (d > 0) durationMs = d
                            }
                            delay(200.milliseconds)
                        }
                    }
                    val fraction = if (durationMs > 0) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f

                    // 顶部弹幕区
                    DanmakuHost(
                        danmakuList = danmakuUiState.danmakuByNote[item.id]?.items.orEmpty(),
                        player = player,
                        enabled = danmakuUiState.enabled,
                        currentUserId = danmakuViewModel.currentUserId,
                        noteAuthorId = item.author.id,
                        selection = danmakuUiState.selection,
                        opacity = uiState.danmakuOpacity,
                        onSelectionChange = danmakuViewModel::onSelectionChange,
                        onDismissBubble = { danmakuViewModel.dismissBubble() },
                        onDelete = { danmakuViewModel.deleteDanmaku(item.id, it.id) },
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .fillMaxWidth()
                            .displayCutoutPadding()
                            .padding(top = 60.dp, start = 60.dp, end = 60.dp),
                    )

                    // 调节模式，显示柱子，隐藏控制层
                    if (adjustMode != null) {
                        val isBrightness = adjustMode == "brightness"
                        VerticalSlider(
                            fraction = if (isBrightness) brightness else volume,
                            icon = if (isBrightness) painterResource(R.drawable.ic_brightness)
                            else rememberVectorPainter(Icons.AutoMirrored.Filled.VolumeUp),
                            onFractionChange = { f ->
                                if (isBrightness) {
                                    brightness = f
                                    brightnessController.set(f)
                                } else {
                                    volume = f
                                    volumeController.set(f)
                                }
                            },
                            modifier = Modifier
                                .align(if (isBrightness) Alignment.CenterStart else Alignment.CenterEnd)
                                .padding(horizontal = 48.dp),
                        )
                        return@VideoPage
                    }

                    // 控制层
                    if (controlsVisible) {
                        // 返回、标题
                        Row(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .displayCutoutPadding()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.video_exit_fullscreen),
                                tint = Color.White,
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .clickable(
                                        indication = null,
                                        interactionSource = remember { MutableInteractionSource() },
                                        onClick = { onExit() },
                                    )
                                    .padding(8.dp),
                            )
                            item.title?.takeIf { it.isNotBlank() }?.let { t ->
                                Text(
                                    text = t,
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    modifier = Modifier
                                        .padding(start = 4.dp)
                                        .width(280.dp),
                                )
                            }
                        }

                        // 中央播放、暂停
                        Box(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.3f))
                                .clickable(
                                    indication = null,
                                    interactionSource = remember { MutableInteractionSource() },
                                    onClick = { viewModel.togglePaused(item.id) },
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = if (userPaused) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                                contentDescription = if (userPaused) stringResource(R.string.pip_play) else stringResource(R.string.pip_pause),
                                tint = Color.White.copy(alpha = 0.5f),
                                modifier = Modifier.size(40.dp),
                            )
                        }

                        // 左侧亮度
                        SideAdjustButton(
                            icon = painterResource(R.drawable.ic_brightness),
                            desc = stringResource(R.string.video_brightness),
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .displayCutoutPadding()
                                .padding(start = 24.dp),
                            onClick = {
                                brightness = brightnessController.current()
                                adjustMode = "brightness"
                            },
                        )

                        // 右侧音量
                        SideAdjustButton(
                            icon = rememberVectorPainter(Icons.AutoMirrored.Filled.VolumeUp),
                            desc = stringResource(R.string.video_volume),
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .displayCutoutPadding()
                                .padding(end = 24.dp),
                            onClick = {
                                volume = volumeController.current()
                                adjustMode = "volume"
                            },
                        )

                        // 底部进度条与动作栏
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
                                onDragStart = {
                                    scrubbing = true
                                    danmakuViewModel.dismissBubble()
                                },
                                onSeek = { v -> player?.seekTo((v * durationMs).toLong()) },
                                onDragEnd = { v ->
                                    player?.seekTo((v * durationMs).toLong())
                                    scrubbing = false
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(24.dp),
                            )
                            // 动作栏
                            LandscapeActionBar(
                                danmakuEnabled = danmakuUiState.enabled,
                                liked = item.liked,
                                likeCount = item.likeCount,
                                favorited = item.favorited,
                                favCount = item.favCount,
                                currentSpeed = uiState.playbackSpeed,
                                onToggleLike = { viewModel.toggleLikeById(item.id) },
                                onToggleFavorite = { viewModel.toggleFavoriteById(item.id) },
                                onToggleDanmaku = {
                                    val wasEnabled = danmakuUiState.enabled
                                    danmakuViewModel.toggleEnabled()
                                    if (wasEnabled) danmakuOffToast.showToast(context) else danmakuOnToast.showToast(context)
                                },
                                onDanmakuBoxClick = { danmakuViewModel.openInput() },
                                onSpeedClick = {
                                    controlsVisible = false// 隐藏控制层
                                    showSpeedDrawer = true
                                },
                            )
                            Spacer(Modifier.height(4.dp))
                        }
                    }
                }
            }
        }

        // 倍速抽屉
        SpeedDrawer(
            visible = showSpeedDrawer,
            currentSpeed = uiState.playbackSpeed,
            onSpeedChange = {
                viewModel.setPlaybackSpeed(it)
                showSpeedDrawer = false
            },
            onDismiss = { showSpeedDrawer = false },
        )

        // 弹幕输入框
        if (danmakuUiState.showInput) {
            val current = items.getOrNull(pagerState.currentPage.coerceIn(0, lastIndex))
            DanmakuInputSheet(
                color = danmakuUiState.color,
                onColorChange = danmakuViewModel::setColor,
                onToggleOff = {
                    danmakuViewModel.toggleEnabled()
                    danmakuViewModel.closeInput()
                    danmakuOffToast.showToast(context)
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

/// 侧边亮度、音量图标
@Composable
private fun SideAdjustButton(
    icon: Painter,
    desc: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.4f))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = icon,
            contentDescription = desc,
            tint = Color.White,
            modifier = Modifier.size(24.dp),
        )
    }
}

/// 底部动作栏
@Composable
private fun LandscapeActionBar(
    danmakuEnabled: Boolean,
    liked: Boolean,
    likeCount: Int,
    favorited: Boolean,
    favCount: Int,
    currentSpeed: Float,
    onToggleLike: () -> Unit,
    onToggleFavorite: () -> Unit,
    onToggleDanmaku: () -> Unit,
    onDanmakuBoxClick: () -> Unit,
    onSpeedClick: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // 点赞
            Box(
                modifier = Modifier.width(63.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    LikeButton(
                        liked = liked,
                        onToggle = onToggleLike,
                        iconSize = 24.dp,
                        animationSize = 32.dp,
                        inactiveColor = Color.White,
                    )
                    Text(
                        text = if (likeCount > 0) likeCount.toString() else stringResource(R.string.video_like),
                        fontSize = 12.sp,
                        color = Color.White,
                    )
                }
            }
            // 收藏
            Box(
                modifier = Modifier.width(63.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    FavoriteButton(
                        favorited = favorited,
                        onToggle = onToggleFavorite,
                        modifier = Modifier.size(32.dp),
                        iconSize = 24.dp,
                        inactiveColor = Color.White,
                    )
                    Text(
                        text = if (favCount > 0) favCount.toString() else stringResource(R.string.video_favorite),
                        fontSize = 12.sp,
                        color = Color.White,
                    )
                }
            }
            Spacer(Modifier.weight(1f))
            // 倍速入口
            Text(
                text = if (currentSpeed == 1f) stringResource(R.string.video_speed) else formatSpeed(currentSpeed),
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                        onClick = onSpeedClick,
                    )
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            )
        }
        // 弹幕框
        Row(
            modifier = Modifier
                .align(Alignment.Center)
                .clip(RoundedCornerShape(50))
                .background(Color.White.copy(alpha = 0.18f))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // 弹幕开关键
            Icon(
                painter = painterResource(R.drawable.ic_barrage),
                contentDescription = if (danmakuEnabled) stringResource(R.string.video_danmaku_disable) else stringResource(R.string.video_danmaku_enable),
                tint = if (danmakuEnabled) Color.White else Color.White.copy(alpha = 0.4f),
                modifier = Modifier
                    .size(22.dp)
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                        onClick = onToggleDanmaku,
                    ),
            )
            // 发弹幕占位
            if (danmakuEnabled) {
                Box(
                    modifier = Modifier
                        .width(150.dp)
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() },
                            onClick = onDanmakuBoxClick,
                        ),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Text(text = stringResource(R.string.video_danmaku_send_prompt), color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
                }
            }
        }
    }
}
