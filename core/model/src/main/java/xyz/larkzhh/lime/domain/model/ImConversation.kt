package xyz.larkzhh.lime.domain.model

/**
 * 私信会话列表项领域模型
 */
data class ImConversation(
    //c2c_<userID>
    val conversationId: String,
    val showName: String,
    val faceUrl: String? = null,
    val lastMessageText: String = "",
    val unreadCount: Int = 0,
    val timestamp: Long = 0L,
)
