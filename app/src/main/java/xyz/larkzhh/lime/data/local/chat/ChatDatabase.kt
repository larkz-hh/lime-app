package xyz.larkzhh.lime.data.local.chat

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * AI 聊天本地缓存数据库
 */
@Database(
    entities = [ConversationEntity::class, MessageEntity::class],
    version = 3,
    exportSchema = false,
)
abstract class ChatDatabase : RoomDatabase() {
    abstract fun chatDao(): ChatDao
}
