package xyz.larkzhh.lime.ui

import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateOf
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.R as Media3R
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import xyz.larkzhh.lime.R
import xyz.larkzhh.lime.core.designsystem.R as DesignSystemR
import xyz.larkzhh.lime.navigation.route.Screen
import xyz.larkzhh.lime.navigation.graph.VideoNavGraph
import xyz.larkzhh.lime.ui.theme.AppLimeTheme
import xyz.larkzhh.lime.ui.video.player.VideoPlayerManager
import xyz.larkzhh.lime.util.text.AppLanguage

/// 打开视频页
fun Context.openVideo(noteId: Long, source: String) {
    VideoActivity.instance?.finish()
    startActivity(
        Intent(this, VideoActivity::class.java).apply {
            putExtra(VideoActivity.EXTRA_NOTE_ID, noteId)
            putExtra(VideoActivity.EXTRA_SOURCE, source)
        }
    )
}

@androidx.annotation.OptIn(UnstableApi::class)
@AndroidEntryPoint
class VideoActivity : ComponentActivity() {

    @Inject
    lateinit var playerManager: VideoPlayerManager

    private val isInPip = mutableStateOf(false)

    private var pipVideoWidth = 9
    private var pipVideoHeight = 16
    private var pipPaused = false

    /// 应用语言
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppLanguage.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        instance = this
        enableEdgeToEdge()
        setContent {
            AppLimeTheme {
                VideoNavGraph(
                    noteId = intent.getLongExtra(EXTRA_NOTE_ID, 0L),
                    source = intent.getStringExtra(EXTRA_SOURCE)
                        ?: Screen.VideoFeed.SOURCE_RECOMMENDATION,
                    isInPip = isInPip.value,
                    playerManager = playerManager,
                    onEnterMiniPlayer = ::enterPip,
                    onPipPausedChanged = ::refreshPipPlayPause,
                    onExit = { finish() },
                )
            }
        }
    }

    /// 进入系统画中画
    private fun enterPip(videoWidth: Int, videoHeight: Int) {
        pipVideoWidth = videoWidth.takeIf { it > 0 } ?: 9
        pipVideoHeight = videoHeight.takeIf { it > 0 } ?: 16
        isInPip.value = true
        val entered = enterPictureInPictureMode(buildPipParams())
        if (!entered) isInPip.value = false
    }

    /// 暂停小窗视频
    private fun refreshPipPlayPause(paused: Boolean) {
        if (pipPaused == paused) return
        pipPaused = paused
        if (isInPip.value) {
            setPictureInPictureParams(buildPipParams())
        }
    }

    /// pip参数配置
    private fun buildPipParams(): PictureInPictureParams {
        val raw = Rational(pipVideoWidth, pipVideoHeight)
        // [1/2.39, 2.39]
        val ratio = if (raw.toFloat() in 0.418f..2.39f) raw else Rational(9, 16)
        val builder = PictureInPictureParams.Builder().setAspectRatio(ratio)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder.setActions(
                listOf(
                    RemoteAction(
                        Icon.createWithResource(
                            this,
                            Media3R.drawable.media3_icon_skip_back_15,
                        ),
                        getString(R.string.pip_seek_back),
                        getString(R.string.pip_seek_back),
                        pipActionPendingIntent(PipMediaActionReceiver.ACTION_SEEK_BACK),
                    ),
                    RemoteAction(
                        Icon.createWithResource(
                            this,
                            if (pipPaused) {
                                Media3R.drawable.media3_icon_play
                            } else {
                                Media3R.drawable.media3_icon_pause
                            },
                        ),
                        getString(if (pipPaused) DesignSystemR.string.pip_play else R.string.pip_pause),
                        getString(if (pipPaused) DesignSystemR.string.pip_play else R.string.pip_pause),
                        pipActionPendingIntent(PipMediaActionReceiver.ACTION_TOGGLE_PLAY),
                    ),
                    RemoteAction(
                        Icon.createWithResource(
                            this,
                            Media3R.drawable.media3_icon_skip_forward_15,
                        ),
                        getString(R.string.pip_seek_forward),
                        getString(R.string.pip_seek_forward),
                        pipActionPendingIntent(PipMediaActionReceiver.ACTION_SEEK_FORWARD),
                    ),
                ),
            )
        }
        return builder.build()
    }

    // 构建广播意图
    private fun pipActionPendingIntent(action: String): PendingIntent =
        PendingIntent.getBroadcast(
            this,
            action.hashCode(),
            Intent(this, PipMediaActionReceiver::class.java).setAction(action),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode)
        isInPip.value = isInPictureInPictureMode
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration,
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        isInPip.value = isInPictureInPictureMode
    }

    override fun onDestroy() {
        super.onDestroy()
        // 防止播放器被释放
        if (instance === this) {
            instance = null
            playerManager.release()
        }
    }

    companion object {
        const val EXTRA_NOTE_ID = "noteId"
        const val EXTRA_SOURCE = "source"

        @Volatile
        var instance: VideoActivity? = null
            private set
    }
}
