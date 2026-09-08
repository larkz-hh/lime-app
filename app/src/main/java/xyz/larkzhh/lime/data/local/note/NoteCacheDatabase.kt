package xyz.larkzhh.lime.data.local.note

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * 笔记内容缓存数据库
 */
@Database(
    entities = [NoteDetailEntity::class, CommentCacheEntity::class, ReplyCacheEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class NoteCacheDatabase : RoomDatabase() {
    abstract fun noteCacheDao(): NoteCacheDao
}
