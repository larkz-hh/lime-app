package xyz.larkzhh.lime.util.system

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

fun Context.findActivity(): Activity? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

/// 横屏沉浸式锁定
@Composable
fun LockLandscapeImmersive(active: Boolean) {
    val context = LocalContext.current
    DisposableEffect(active) {
        val activity = context.findActivity()
        if (activity == null || !active) {
            return@DisposableEffect onDispose { }
        }
        val window = activity.window
        val originalOrientation = activity.requestedOrientation
        val controller = WindowInsetsControllerCompat(window, window.decorView)

        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE// 锁横屏
        // 隐藏系统栏，上滑临时唤出
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.systemBars())

        onDispose {
            // 恢复方向与系统栏
            activity.requestedOrientation = originalOrientation
            controller.show(WindowInsetsCompat.Type.systemBars())
        }
    }
}
