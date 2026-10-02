package xyz.larkzhh.lime.ui.video.player

import android.view.TextureView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import xyz.larkzhh.lime.R

/// 单页视频
@UnstableApi
@Composable
fun VideoPage(
    noteId: Long,
    playUrl: String,
    width: Int,
    height: Int,
    isActive: Boolean,
    userPaused: Boolean,
    playerManager: VideoPlayerManager,
    onTogglePlay: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    controlEnabled: Boolean = true,// 是否由本页掌控该播放器
    forcePaused: Boolean = false,// 外部强制暂停
    showPauseIcon: Boolean = true,// 是否播放图标
    playbackSpeed: Float = 1f,// 播放倍速
    autoPlayNext: Boolean = false,// 自动连播
    backgroundAudio: Boolean = false,// 后台继续播放音频
    onPlaybackEnded: () -> Unit = {},// 播放结尾回调
    gestureModifier: Modifier? = null,// 自定义手势层
    content: @Composable BoxScope.(player: ExoPlayer?) -> Unit = {},// chrome 浮层
) {
    val context = LocalContext.current
    val mediaItem = remember(playUrl, title) {
        MediaItem.Builder()
            .setUri(playUrl)
            .setMediaMetadata(MediaMetadata.Builder().setTitle(title).build())
            .build()
    }
    //  全屏时竖屏页让出播放器持有控制
    val player = if (controlEnabled) {
        remember(noteId) { playerManager.getOrCreate(noteId, mediaItem) }
    } else {
        null
    }

    // 感知前后台
    val lifecycleOwner = LocalLifecycleOwner.current
    var isForeground by remember { mutableStateOf(true) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> isForeground = true
                Lifecycle.Event.ON_STOP -> isForeground = false
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(player, isActive, userPaused, controlEnabled, forcePaused, isForeground, backgroundAudio) {
        val canPlay = isActive && !userPaused && !forcePaused && (isForeground || backgroundAudio)
        if (controlEnabled) player?.playWhenReady = canPlay
    }

    // 离开页面（切走 tab / 退出视频页）时暂停播放；
    // 进入横屏全屏时 controlEnabled 已转 false（控制权交接），不暂停
    val latestControlEnabled by rememberUpdatedState(controlEnabled)
    DisposableEffect(Unit) {
        onDispose {
            if (latestControlEnabled) player?.playWhenReady = false
        }
    }

    // 上报活跃页
    LaunchedEffect(player, noteId, isActive, controlEnabled) {
        if (isActive && controlEnabled) playerManager.setActivePlayer(noteId)
    }

    // 应用倍速
    LaunchedEffect(player, playbackSpeed, controlEnabled) {
        if (controlEnabled) player?.setPlaybackSpeed(playbackSpeed)
    }

    // 自动连播
    val currentOnEnded by rememberUpdatedState(onPlaybackEnded)
    DisposableEffect(player, isActive, autoPlayNext, controlEnabled) {
        val p = player?.takeIf { controlEnabled } ?: return@DisposableEffect onDispose { }
        p.repeatMode = if (autoPlayNext) Player.REPEAT_MODE_OFF else Player.REPEAT_MODE_ONE
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_ENDED && isActive && autoPlayNext) currentOnEnded()
            }
        }
        p.addListener(listener)
        onDispose { p.removeListener(listener) }
    }

    // 服务端视频比例
    var videoRatio by remember(playUrl) {
        mutableFloatStateOf(if (width > 0 && height > 0) width.toFloat() / height else 0f)
    }
    DisposableEffect(player) {
        val p = player ?: return@DisposableEffect onDispose { }
        val listener = object : Player.Listener {
            override fun onVideoSizeChanged(videoSize: VideoSize) {
                // 实际比例
                val w = videoSize.width * videoSize.pixelWidthHeightRatio
                val h = videoSize.height.toFloat()
                if (w > 0f && h > 0f) videoRatio = w / h
            }
        }
        p.addListener(listener)
        onDispose { p.removeListener(listener) }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .clipToBounds()
            .then(
                gestureModifier ?: Modifier.clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = onTogglePlay,
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        val textureView = remember { TextureView(context) }
        DisposableEffect(player, controlEnabled) {
            if (controlEnabled) player?.setVideoTextureView(textureView)
            onDispose {
                if (controlEnabled) player?.clearVideoTextureView(textureView)
            }
        }
        AndroidView(
            factory = { textureView },
            modifier = when {
                videoRatio > 0f && videoRatio < 1f ->
                    Modifier.fillMaxWidth().aspectRatio(videoRatio)
                videoRatio > 0f -> Modifier.aspectRatio(videoRatio)
                else -> Modifier.fillMaxSize()
            },
        )

        // 暂停图标
        if (userPaused && showPauseIcon) {
            Box(
                modifier = Modifier
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

        // chrome 浮层
        content(player)
    }
}

/// 跟随小窗播放状态同步暂停态
@UnstableApi
@Composable
fun SyncPiPPlayState(
    noteId: Long,
    playerManager: VideoPlayerManager,
    onPausedChanged: (Boolean) -> Unit,
) {
    DisposableEffect(noteId, playerManager) {
        val player = playerManager.playerOf(noteId) ?: return@DisposableEffect onDispose {}
        val listener = object : Player.Listener {
            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                onPausedChanged(!playWhenReady)
            }
        }
        player.addListener(listener)
        onPausedChanged(!player.playWhenReady)// 同步当前状态
        onDispose { player.removeListener(listener) }
    }
}
