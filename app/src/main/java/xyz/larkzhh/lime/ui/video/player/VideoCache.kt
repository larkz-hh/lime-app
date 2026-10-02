package xyz.larkzhh.lime.ui.video.player

import android.content.Context
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.database.StandaloneDatabaseProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 视频磁盘缓存
 */
@UnstableApi
@Singleton
class VideoCache @Inject constructor(
    @ApplicationContext context: Context,
) {

    /// Exoplayer 本地缓存配置
    private val simpleCache: SimpleCache by lazy {
        SimpleCache(
            File(context.cacheDir, "video_cache"),
            LeastRecentlyUsedCacheEvictor(512L * 1024 * 1024),
            StandaloneDatabaseProvider(context),//
        )
    }

    // 缓存功能的网络数据源工厂
    val dataSourceFactory: CacheDataSource.Factory by lazy {
        CacheDataSource.Factory()
            .setCache(simpleCache)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)// 读取失败回退网络
            .setUpstreamDataSourceFactory(DefaultHttpDataSource.Factory())
    }
}
