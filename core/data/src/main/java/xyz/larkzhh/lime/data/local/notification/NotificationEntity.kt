package xyz.larkzhh.lime.data.local.notification

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 站内通知本地缓存实体
 */
@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey val id: Long,
    val type: Int,
    val noteId: Long?,
    val commentId: Long?,
    val content: String?,
    val parentCommentId: Long?,
    val replyToContent: String?,
    val noteCover: String?,
    val isRead: Boolean,
    val createTime: String,
    val senderId: Long?,
    val senderNickname: String?,
    val senderAvatar: String?,
)
