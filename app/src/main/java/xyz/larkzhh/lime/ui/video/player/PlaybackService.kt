package xyz.larkzhh.lime.ui.video.player

import androidx.media3.common.util.UnstableApi
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.R as Media3R
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

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
        playerManager.sessionForService()

    override fun onDestroy() {
        instance = null
        super.onDestroy()
    }

    companion object {
        const val CHANNEL_ID = "video_playback"

        @Volatile
        var instance: PlaybackService? = null
            private set
    }
}
