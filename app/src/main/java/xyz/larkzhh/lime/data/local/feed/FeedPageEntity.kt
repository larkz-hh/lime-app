package xyz.larkzhh.lime.data.local.feed

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 首页信息流分页缓存实体
 */
@Entity(tableName = "feed_pages")
data class FeedPageEntity(
    @PrimaryKey val cursorKey: Long,
    val json: String,
    val cachedAt: Long,
)
