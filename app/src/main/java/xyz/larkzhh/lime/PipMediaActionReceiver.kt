package xyz.larkzhh.lime

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import xyz.larkzhh.lime.ui.video.player.VideoPlayerManager


/**
 * 画中画模式媒体控制广播接收器
 */
@OptIn(UnstableApi::class)
@AndroidEntryPoint
class PipMediaActionReceiver : BroadcastReceiver() {

    @Inject
    lateinit var playerManager: VideoPlayerManager

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_SEEK_BACK -> playerManager.seekActiveBack()
            ACTION_SEEK_FORWARD -> playerManager.seekActiveForward()
            ACTION_TOGGLE_PLAY -> playerManager.toggleActivePlayPause()
        }
    }

    companion object {
        const val ACTION_SEEK_BACK = "xyz.larkzhh.lime.pip.SEEK_BACK"
        const val ACTION_SEEK_FORWARD = "xyz.larkzhh.lime.pip.SEEK_FORWARD"
        const val ACTION_TOGGLE_PLAY = "xyz.larkzhh.lime.pip.TOGGLE_PLAY"
    }
}
