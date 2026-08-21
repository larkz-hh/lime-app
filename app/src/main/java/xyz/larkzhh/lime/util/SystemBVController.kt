package xyz.larkzhh.lime.util

import android.content.Context
import android.media.AudioManager
import android.provider.Settings
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/// 系统音量控制器
class VolumeController(context: Context) {
    private val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    val max: Int get() = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC)

    /// 当前音量
    fun current(): Float {
        val m = max
        return if (m > 0) audio.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / m else 0f
    }

    /// 设置音量
    fun set(fraction: Float) {
        val v = (fraction.coerceIn(0f, 1f) * max).toInt().coerceIn(0, max)
        audio.setStreamVolume(AudioManager.STREAM_MUSIC, v, 0)// 不显示系统弹窗
    }
}

/// 屏幕亮度控制器
class BrightnessController(context: Context) {
    private val activity = context.findActivity()

    /// 当前亮度
    fun current(): Float {
        val attr = activity?.window?.attributes
        val v = attr?.screenBrightness ?: -1f
        if (v in 0f..1f) return v
        // 未覆盖时读系统当前的亮度
        val ctx = activity ?: return 0.5f
        return try {
            val sys = Settings.System.getInt(ctx.contentResolver, Settings.System.SCREEN_BRIGHTNESS)
            (sys / 255f).coerceIn(0f, 1f)
        } catch (e: Settings.SettingNotFoundException) {
            0.5f
        }
    }

    /// 设置窗口亮度
    fun set(fraction: Float) {
        val window = activity?.window ?: return
        window.attributes = window.attributes.apply {
            screenBrightness = fraction.coerceIn(0.01f, 1f)
        }
    }

    /// 恢复为跟随系统亮度
    fun reset() {
        val window = activity?.window ?: return
        window.attributes = window.attributes.apply {
            screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE// 跟随系统
        }
    }
}

@Composable
fun rememberVolumeController(): VolumeController {
    val context = LocalContext.current
    return remember { VolumeController(context) }
}

@Composable
fun rememberBrightnessController(): BrightnessController {
    val context = LocalContext.current
    return remember { BrightnessController(context) }
}
