package xyz.larkzhh.lime.data.local.chat

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * AI 会话缓存实体
 */
@Entity(tableName = "ai_conversations")
data class ConversationEntity(
    @PrimaryKey val id: String,
    val title: String,
    val updateTime: Long,
)

/**
 * AI 消息缓存实体
 */
@Entity(
    tableName = "ai_messages",
    indices = [Index("conversationId")],
)
data class MessageEntity(
    @PrimaryKey(autoGenerate = true) val localId: Long = 0L,
    val conversationId: String,
    val serverId: Long? = null,
    val clientId: String? = null,
    val role: String, // user / assistant
    val content: String = "",
    val images: String? = null,
    val localImageUris: String? = null,
    val status: String = "DONE",
    val createTime: Long,
)
