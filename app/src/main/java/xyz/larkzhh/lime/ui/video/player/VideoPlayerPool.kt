package xyz.larkzhh.lime.ui.video.player

import android.content.Context
import android.view.TextureView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

// 播放器池
@UnstableApi
class VideoPlayerManager(
    private val context: Context,
    private val maxPlayers: Int = 4,
) {
    // LRU 缓存
    private val cache = object : LinkedHashMap<Long, ExoPlayer>(0, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Long, ExoPlayer>): Boolean {
            if (size > maxPlayers) {
                eldest.value.release()
                return true
            }
            return false
        }
    }

    // 创建与复用播放器
    fun getOrCreate(id: Long, mediaItem: MediaItem): ExoPlayer =
        cache[id] ?: ExoPlayer.Builder(context).build().also { p ->
            p.setMediaItem(mediaItem)
            p.repeatMode = Player.REPEAT_MODE_ONE
            p.prepare()
            cache[id] = p
        }

    /// 读取指定视频当前播放进度
    fun currentPositionOf(id: Long): Long = cache[id]?.currentPosition ?: 0L

    // 释放全部播放器
    fun release() {
        cache.values.forEach { it.release() }
        cache.clear()
    }
}

@UnstableApi
@Composable
fun rememberVideoPlayerManager(maxPlayers: Int = 4): VideoPlayerManager {
    val context = LocalContext.current
    val manager = remember { VideoPlayerManager(context, maxPlayers) }
    DisposableEffect(manager) {
        onDispose { manager.release() }
    }
    return manager
}

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
    controlEnabled: Boolean = true,// 是否由本页掌控该播放器
    forcePaused: Boolean = false,// 外部强制暂停
    showPauseIcon: Boolean = true,// 是否播放图标
    playbackSpeed: Float = 1f,// 播放倍速
    autoPlayNext: Boolean = false,// 自动连播
    onPlaybackEnded: () -> Unit = {},// 播放结尾回调
    gestureModifier: Modifier? = null,// 自定义手势层
    content: @Composable BoxScope.(player: ExoPlayer?) -> Unit = {},// chrome 浮层
) {
    val context = LocalContext.current
    val mediaItem = remember(playUrl) { MediaItem.fromUri(playUrl) }
    //  全屏时竖屏页让出播放器持有控制
    val player = if (controlEnabled) {
        remember(noteId) { playerManager.getOrCreate(noteId, mediaItem) }
    } else {
        null
    }

    // 感知前后台
    val lifecycleOwner =LocalLifecycleOwner.current
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

    LaunchedEffect(player, isActive, userPaused, controlEnabled, forcePaused, isForeground) {
        if (controlEnabled) player?.playWhenReady = isActive && !userPaused && !forcePaused && isForeground
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
            modifier = if (videoRatio > 0f) {
                Modifier.aspectRatio(videoRatio)
            } else {
                Modifier.fillMaxSize()
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
                    contentDescription = "播放",
                    tint = Color.White,
                    modifier = Modifier.size(40.dp),
                )
            }
        }

        // chrome 浮层
        content(player)
    }
}
