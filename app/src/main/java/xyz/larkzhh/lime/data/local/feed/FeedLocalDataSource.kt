package xyz.larkzhh.lime.data.local.feed

import com.google.gson.Gson
import xyz.larkzhh.lime.data.network.model.FeedResponse
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 首页信息流本地数据源
 */
@Singleton
class FeedLocalDataSource @Inject constructor(
    private val feedDao: FeedDao,
) {

    private val gson = Gson()

    /// 读取缓存的首页信息流
    suspend fun getFirstPage(): FeedResponse? = runCatching {
        feedDao.get(FeedDao.FIRST_PAGE_KEY)
            ?.let { gson.fromJson(it.json, FeedResponse::class.java) }
    }.getOrNull()

    /// 写入首页信息流缓存
    suspend fun saveFirstPage(page: FeedResponse) {
        runCatching {
            feedDao.upsert(
                FeedPageEntity(
                    cursorKey = FeedDao.FIRST_PAGE_KEY,
                    json = gson.toJson(page),
                    cachedAt = System.currentTimeMillis(),
                )
            )
        }
    }
}
