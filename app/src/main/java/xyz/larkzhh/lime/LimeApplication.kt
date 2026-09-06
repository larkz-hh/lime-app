package xyz.larkzhh.lime

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.media3.common.util.UnstableApi
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import coil3.video.VideoFrameDecoder
import okio.Path.Companion.toOkioPath
import com.tencent.mmkv.MMKV
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import xyz.larkzhh.lime.ui.video.player.VideoPlayerManager
import xyz.larkzhh.lime.util.AppLanguage
import xyz.larkzhh.lime.work.TranslatePrefetchWorker
import xyz.larkzhh.lime.work.WidgetHotRefreshWorker

@androidx.annotation.OptIn(UnstableApi::class)
@HiltAndroidApp
class LimeApplication : Application(), SingletonImageLoader.Factory {

    @Inject
    lateinit var playerManager: VideoPlayerManager

    /// 应用语言
    override fun attachBaseContext(base: Context) {
        MMKV.initialize(base)
        super.attachBaseContext(AppLanguage.wrap(base))
    }

    override fun onCreate() {
        super.onCreate()
        MMKV.initialize(this)
        // 首次启动后台预下载
        TranslatePrefetchWorker.enqueueOnFirstLaunch(this)
        // 桌面小组件热搜定时刷新
        WidgetHotRefreshWorker.ensureScheduled(this)
        // 跟踪应用前后台
        var startedCount = 0// 可见数量
        val handler = Handler(Looper.getMainLooper())
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityStarted(activity: Activity) {
                handler.removeCallbacksAndMessages(null)
                startedCount++
                playerManager.onAppForegroundChanged(true)// 通知播放器管理器
            }

            override fun onActivityStopped(activity: Activity) {
                startedCount--
                handler.removeCallbacksAndMessages(null)
                handler.postDelayed({
                    if (startedCount <= 0) playerManager.onAppForegroundChanged(false)
                }, 600L)
            }

            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
            override fun onActivityResumed(activity: Activity) = Unit
            override fun onActivityPaused(activity: Activity) = Unit
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
            override fun onActivityDestroyed(activity: Activity) = Unit
        })
    }

    /// 视频帧解码器，封面按时间戳取帧
    override fun newImageLoader(context: PlatformContext): ImageLoader =
        ImageLoader.Builder(context)
            .components { add(VideoFrameDecoder.Factory()) }// 注册视频帧解码器
            .memoryCache {
                MemoryCache.Builder()
                    .maxSizePercent(context, 0.25)
                    .build()
            }// 内存缓存
            .diskCache {
                DiskCache.Builder()
                    .directory(context.cacheDir.resolve("image_cache").toOkioPath())
                    .maxSizeBytes(256L * 1024 * 1024)
                    .build()
            }// 磁盘缓存
            .build()
}
