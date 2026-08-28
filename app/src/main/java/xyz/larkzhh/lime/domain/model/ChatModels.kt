package xyz.larkzhh.lime.domain.model

/// 聊天消息角色
enum class ChatRole { USER, ASSISTANT }

/// 聊天消息状态
enum class ChatMessageStatus {
    SENDING,
    STREAMING,
    DONE,
    FAILED,
    STOPPED,
}

/// 本地临时会话 id 基数
const val TEMP_CONVERSATION_ID_BASE = -1_000_000L

/// 聊天消息域模型。
data class ChatMessage(
    val localId: Long = 0L,// 未落库
    val conversationId: Long,
    val serverId: Long? = null,
    val role: ChatRole,
    val content: String = "",
    val images: List<String> = emptyList(),
    val localImageUris: List<String> = emptyList(),
    val status: ChatMessageStatus = ChatMessageStatus.DONE,
    val createTime: Long = System.currentTimeMillis(),
    val attemptCount: Int = 0,
    val nextRetryAt: Long = 0L,
)

/// 会话域模型
data class ChatConversation(
    val id: Long,
    val title: String,
    val updateTime: Long = System.currentTimeMillis(),
)

/// AI 聊天流式事件
sealed interface ChatStreamEvent {
    data class Delta(val content: String) : ChatStreamEvent
    data class Done(
        val conversationId: Long,
        val userMessageId: Long?,
        val assistantMessageId: Long?,
        val model: String?,
    ) : ChatStreamEvent
    data class Error(val message: String) : ChatStreamEvent
}

/// AI 模型信息
data class AiModelInfo(
    val name: String,
    val displayName: String,
    val description: String? = null,
    val supportsVision: Boolean = false,
)
