package xyz.larkzhh.lime.ui.video.player

import android.app.NotificationManager
import android.content.Context
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.R as Media3R
import com.google.common.util.concurrent.ListenableFuture
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import xyz.larkzhh.lime.R

/**
 * 后台播放服务
 */
@UnstableApi
@AndroidEntryPoint
class PlaybackService : MediaSessionService() {

    @Inject
    lateinit var playerManager: VideoPlayerManager

    override fun onCreate() {
        super.onCreate()
        instance = this
        setShowNotificationForIdlePlayer(SHOW_NOTIFICATION_FOR_IDLE_PLAYER_ALWAYS)// 播放暂停仍显示
        setMediaNotificationProvider(
            DefaultMediaNotificationProvider.Builder(this)
                .setChannelId(CHANNEL_ID)
                .setChannelName(R.string.playback_channel_name)
                .build()
                .also { provider ->
                    provider.setSmallIcon(
                        Media3R.drawable.media_session_service_notification_ic_music_note,
                    )
                },
        )
        // 创建会话
        playerManager.sessionForService()?.let { addSession(it) }
    }

    /// 强制前台
    override fun onUpdateNotificationAsync(
        session: MediaSession,
        startInForegroundRequired: Boolean,
    ): ListenableFuture<Void?> =
        super.onUpdateNotificationAsync(session, true)

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
        playerManager.sessionForService()

    override fun onDestroy() {
        instance = null
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .cancel(NOTIFICATION_ID)
        super.onDestroy()
    }

    companion object {
        const val CHANNEL_ID = "video_playback"
        const val NOTIFICATION_ID = 1001
        @Volatile
        var instance: PlaybackService? = null
            private set
    }
}
