package xyz.larkzhh.lime.data.local.feed

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 信息流逐条缓存实体
 */
@Entity(tableName = "feed_items", indices = [Index("feedKey")])
data class FeedItemEntity(
    @PrimaryKey(autoGenerate = true) val rowId: Long = 0L,
    val feedKey: String,
    val noteId: Long,
    val sortIndex: Int,
    val json: String,
)

/**
 * 信息流游标缓存实体
 */
@Entity(tableName = "feed_cursors")
data class FeedCursorEntity(
    @PrimaryKey val feedKey: String,
    val nextCursor: Long?,
)
