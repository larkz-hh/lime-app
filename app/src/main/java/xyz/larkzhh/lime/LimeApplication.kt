package xyz.larkzhh.lime

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
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
import xyz.larkzhh.lime.navigation.MainEntryPoint
import xyz.larkzhh.lime.navigation.VideoOpener
import xyz.larkzhh.lime.ui.MainActivity
import xyz.larkzhh.lime.ui.openVideo
import xyz.larkzhh.lime.ui.videoIntent
import xyz.larkzhh.lime.ui.video.player.VideoPlayerManager
import xyz.larkzhh.lime.util.text.AppLanguage
import xyz.larkzhh.lime.work.TranslatePrefetchWorker
import xyz.larkzhh.lime.ui.widget.WidgetHotRefreshWorker

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
        VideoOpener.open = { context, noteId, source -> context.openVideo(noteId, source) }
        VideoOpener.intent = { context, noteId, source -> videoIntent(context, noteId, source) }
        MainEntryPoint.intent = { context, action ->
            Intent(context, MainActivity::class.java)
                .setAction(action)
                .addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TOP or
                            Intent.FLAG_ACTIVITY_SINGLE_TOP
                )
        }
        // 首次启动后台预下载
        TranslatePrefetchWorker.enqueueOnFirstLaunch(this)
        // 桌面小组件热搜定时刷新
        WidgetHotRefreshWorker.ensureScheduled(this)
        // 跟踪应用前后台
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                playerManager.onAppForegroundChanged(true)// 通知播放器管理器
            }

            override fun onStop(owner: LifecycleOwner) {
                playerManager.onAppForegroundChanged(false)// 通知播放器管理器
            }
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
