package xyz.larkzhh.lime.ui.video.player

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaSession
import javax.inject.Inject
import javax.inject.Singleton
import xyz.larkzhh.lime.ui.VideoActivity

private const val PIP_SEEK_STEP_MS = 15_000L// 小窗跳转步长

/**
 * 播放器池
 */
@UnstableApi
@Singleton
class VideoPlayerManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val videoCache: VideoCache,
) {
    private val maxPlayers = 4

    // 系统媒体会话
    private var session: MediaSession? = null
    private var sessionNoteId: Long = -1L
    private val sessionWrappers = HashMap<Long, PipSeekPlayer>()
    private var appInForeground = true// 应用是否在前台

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
        cache[id] ?: ExoPlayer.Builder(context)
            .setMediaSourceFactory(DefaultMediaSourceFactory(videoCache.dataSourceFactory))
            .build()
            .also { p ->
                p.setMediaItem(mediaItem)
                p.repeatMode = Player.REPEAT_MODE_ONE
                p.setSeekBackIncrementMs(PIP_SEEK_STEP_MS)
                p.setSeekForwardIncrementMs(PIP_SEEK_STEP_MS)
                p.prepare()
                cache[id] = p
            }

    /// 读取指定视频当前播放进度
    fun currentPositionOf(id: Long): Long = cache[id]?.currentPosition ?: 0L

    /// 取已缓存的播放器
    fun playerOf(id: Long): ExoPlayer? = cache[id]

    /// 标记当前活跃的视频页
    fun setActivePlayer(id: Long) {
        if (sessionNoteId == id && session != null) return
        val player = cache[id] ?: return
        sessionNoteId = id
        val wrapper = sessionWrappers.getOrPut(id) { PipSeekPlayer(player) }
        ensureSession(wrapper).apply {
            this.player = wrapper
            setSessionActivity(
                PendingIntent.getActivity(
                    context,
                    0,
                    Intent(context, VideoActivity::class.java)
                        .putExtra(VideoActivity.EXTRA_NOTE_ID, id)
                        .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                ),
            )
        }
    }

    private fun ensureSession(player: Player): MediaSession =
        session ?: MediaSession.Builder(context, player).build().also { s ->
            session = s
            // 前台不启动服务、不挂通知，退到后台才由 onAppForegroundChanged 启动
            if (!appInForeground) {
                PlaybackService.instance?.addSession(s)
                    ?: ContextCompat.startForegroundService(
                        context,
                        Intent(context, PlaybackService::class.java),
                    )
            }
        }

    /// 监听并处理应用前后台切换
    fun onAppForegroundChanged(foreground: Boolean) {
        appInForeground = foreground
        if (foreground) {
            if (PlaybackService.instance != null) {
                context.stopService(Intent(context, PlaybackService::class.java))
            }
        } else {
            val s = session
            if (s != null && PlaybackService.instance == null && s.player.playWhenReady) {
                runCatching {
                    ContextCompat.startForegroundService(
                        context,
                        Intent(context, PlaybackService::class.java),
                    )
                }
            }
        }
    }

    /// 返回当前会话
    fun sessionForService(): MediaSession? = session

    /// 后退 15 秒
    fun seekActiveBack() {
        session?.player?.seekBack()
    }

    /// 前进 15 秒
    fun seekActiveForward() {
        session?.player?.seekForward()
    }

    /// 暂停、恢复
    fun toggleActivePlayPause() {
        val player = session?.player ?: return
        if (player.playWhenReady) player.pause() else player.play()
    }

    // 释放全部播放器
    fun release() {
        session?.let { PlaybackService.instance?.removeSession(it) }
        context.stopService(Intent(context, PlaybackService::class.java))
        session?.release()
        session = null
        sessionNoteId = -1L
        sessionWrappers.clear()
        cache.values.forEach { it.release() }
        cache.clear()
    }
}

/// 会话包装播放器
@UnstableApi
private class PipSeekPlayer(delegate: ExoPlayer) : ForwardingPlayer(delegate) {

    override fun getAvailableCommands(): Player.Commands =
        super.getAvailableCommands().buildUpon()
            .add(COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
            .add(COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
            .build()

    override fun seekToPreviousMediaItem() {
        seekBack()
    }

    override fun seekToNextMediaItem() {
        seekForward()
    }
}
