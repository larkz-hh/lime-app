package xyz.larkzhh.lime.data.local.feed

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * 内容缓存数据库
 */
@Database(
    entities = [FeedPageEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class FeedDatabase : RoomDatabase() {
    abstract fun feedDao(): FeedDao
}
