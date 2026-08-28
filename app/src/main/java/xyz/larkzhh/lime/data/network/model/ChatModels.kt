package xyz.larkzhh.lime.data.network.model

/// AI 聊天请求体 SSE
data class AiChatRequest(
    val message: String,
    val conversationId: Long? = null,
    val imageUrls: List<String>? = null,
    val model: String? = null,
)

/// AI 模型信息
data class AiModelDto(
    val name: String,
    val displayName: String,
    val description: String? = null,
    val supportsVision: Boolean = false,
)

/// 会话列表条目
data class ConversationDto(
    val id: Long,
    val title: String,
    val createTime: String? = null,
    val updateTime: String? = null,
)

/// 会话列表分页数据
data class ConversationListData(
    val items: List<ConversationDto>,
    val nextCursor: String? = null,
    val hasMore: Boolean = false,
)

/// 会话历史消息
data class ChatMessageDto(
    val id: Long,
    val role: String,// user / assistant
    val content: String,
    val images: List<String>? = null,
    val noteId: Long? = null,
    val createTime: String? = null,
)
