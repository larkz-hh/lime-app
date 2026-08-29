package xyz.larkzhh.lime.domain.repository

import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import xyz.larkzhh.lime.domain.model.AiModelInfo
import xyz.larkzhh.lime.domain.model.ChatConversation
import xyz.larkzhh.lime.domain.model.ChatMessage
import xyz.larkzhh.lime.domain.model.ChatMessageStatus
import xyz.larkzhh.lime.domain.model.ChatStreamEvent


interface ChatRepository {

    /// AI 聊天 SSE 流
    fun chatStream(
        conversationId: String,
        messageClientId: String,
        message: String,
        imageUrls: List<String>?,
        model: String?,
    ): Flow<ChatStreamEvent>

    /// 拉取模型
    suspend fun fetchModels(): Result<List<AiModelInfo>>

    /// 打断正在生成的回复
    suspend fun cancelGeneration(messageClientId: String, partialContent: String?): Result<Unit>

    /// 远端历史消息合并进本地缓存；返回服务端是否仍在生成（true=需继续轮询）
    suspend fun syncMessages(conversationId: String): Result<Boolean>

    /// 删除远端会话、消息
    suspend fun deleteConversationRemote(conversationId: String): Result<Unit>
    suspend fun deleteMessageRemote(conversationId: String, messageId: Long): Result<Unit>
    suspend fun clearMessagesRemote(conversationId: String): Result<Unit>

    /// 会话列表分页流
    fun conversationsPager(): Flow<PagingData<ChatConversation>>

    /// 观察实时信息
    fun observeLocalMessages(conversationId: String): Flow<List<ChatMessage>>

    /// 读取本地单个会话
    suspend fun getLocalConversation(conversationId: String): ChatConversation?

    /// 最近一次更新的会话
    suspend fun getLatestConversation(): ChatConversation?

    /// 写入会话、消息
    suspend fun saveMessage(message: ChatMessage): Long
    suspend fun saveMessages(messages: List<ChatMessage>)
    suspend fun saveConversation(conversation: ChatConversation)

    /// 更新消息
    suspend fun updateMessageStatus(localId: Long, status: ChatMessageStatus)
    suspend fun updateMessage(localId: Long, serverId: Long?, content: String, status: ChatMessageStatus)
    suspend fun updateMessageWithImages(
        localId: Long,
        serverId: Long?,
        content: String,
        images: List<String>?,
        status: ChatMessageStatus,
    )

    /// 删除本地会话、消息
    suspend fun deleteLocalConversation(conversationId: String)
    suspend fun deleteLocalMessage(localId: Long)
    suspend fun clearLocalMessages(conversationId: String)

    /// 将残留消息标记为失败
    suspend fun resetStaleMessages()

}
