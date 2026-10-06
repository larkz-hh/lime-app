package xyz.larkzhh.lime.data.network.model

/// UserSig 下发响应
data class ImUserSigData(
    val sdkAppId: Long,
    val userId: String,
    val userSig: String,
    val expire: Long,
)

/// 打开私信会话请求
data class ConversationOpenRequest(
    val targetUserId: Long,
)

/// 打开私信会话响应
data class ConversationOpenData(
    val conversationId: String,
)
