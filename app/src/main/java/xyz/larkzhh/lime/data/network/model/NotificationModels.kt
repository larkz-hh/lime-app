package xyz.larkzhh.lime.data.network.model

/// 单条通知
data class NotificationData(
    val id: Long,
    val type: Int,
    val noteId: Long? = null,
    val commentId: Long? = null,
    val content: String? = null,
    val parentCommentId: Long? = null,
    val replyToContent: String? = null,
    val noteCover: String? = null,
    val isRead: Boolean = false,
    val createTime: String? = null,
    val senderId: Long? = null,
    val senderNickname: String? = null,
    val senderAvatar: String? = null,
)

/// 通知列表分页数据
data class NotificationListResponse(
    val items: List<NotificationData>,
    val nextCursor: String? = null,
    val hasMore: Boolean = false,
)

/// 分组未读通知数
data class UnreadCountData(
    val total: Int = 0,
    val likeFav: Int = 0,
    val follow: Int = 0,
    val comment: Int = 0,
)
