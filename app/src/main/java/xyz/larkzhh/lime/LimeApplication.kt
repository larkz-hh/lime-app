package xyz.larkzhh.lime

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.video.VideoFrameDecoder
import com.tencent.mmkv.MMKV
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class LimeApplication : Application(), SingletonImageLoader.Factory {
    override fun onCreate() {
        super.onCreate()
        MMKV.initialize(this)
    }

    /// 视频帧解码器，封面按时间戳取帧
    override fun newImageLoader(context: PlatformContext): ImageLoader =
        ImageLoader.Builder(context)
            .components { add(VideoFrameDecoder.Factory()) }// 注册视频帧解码器
            .build()
}
