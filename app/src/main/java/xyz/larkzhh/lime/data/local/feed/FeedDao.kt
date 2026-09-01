package xyz.larkzhh.lime.data.local.feed

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

/**
 * 信息流缓存 DAO
 */
@Dao
interface FeedDao {

    companion object {
        const val FIRST_PAGE_KEY = -1L// 首页缓存键
    }

    @Query("SELECT * FROM feed_pages WHERE cursorKey = :cursorKey")
    suspend fun get(cursorKey: Long): FeedPageEntity?

    @Upsert
    suspend fun upsert(page: FeedPageEntity)

    @Query("DELETE FROM feed_pages")
    suspend fun clear()
}
