package xyz.larkzhh.lime.data.repository.chat

import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.OkHttpClient
import xyz.larkzhh.lime.data.network.ApiService
import xyz.larkzhh.lime.data.network.collectSse
import xyz.larkzhh.lime.data.network.model.AiChatRequest
import xyz.larkzhh.lime.domain.model.AiModelInfo
import xyz.larkzhh.lime.domain.model.ChatConversation
import xyz.larkzhh.lime.domain.model.ChatMessage
import xyz.larkzhh.lime.domain.model.ChatStreamEvent
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * AI 聊天远端数据源
 */
@Singleton
class ChatRemoteDataSource @Inject constructor(
    private val apiService: ApiService,
    @Named("sse") private val sseClient: OkHttpClient,
    @Named("base_url") private val baseUrl: String,
) {

    private val gson = Gson()

    /// 流式聊天
    fun chatStream(
        conversationId: String,
        messageClientId: String,
        message: String,
        imageUrls: List<String>?,
        model: String?,
    ): Flow<ChatStreamEvent> = flow {
        collectSse(
            client = sseClient,
            url = "${baseUrl}api/ai/chat",
            jsonBody = gson.toJson(AiChatRequest(conversationId, messageClientId, message, imageUrls, model)),
        ) { dto ->
            when (dto.type) {
                "delta" -> dto.content?.let { ChatStreamEvent.Delta(it) }
                "done" -> ChatStreamEvent.Done(dto.userMessageId, dto.assistantMessageId, dto.model)
                "error" -> ChatStreamEvent.Error(dto.message ?: "AI 服务暂时不可用")
                else -> null
            }
        }
    }.flowOn(Dispatchers.IO)

    /// 获取模型
    suspend fun fetchModels(): Result<List<AiModelInfo>> = runCatching {
        val response = apiService.getAiModels()
        check(response.code == 200 && response.data != null) { response.message }
        response.data.map {
            AiModelInfo(it.name, it.displayName, it.description, it.supportsVision)
        }
    }

    /// 获取会话列表一页
    suspend fun fetchConversations(
        cursor: String?,
        size: Int,
    ): Result<Pair<List<ChatConversation>, String?>> = runCatching {
        val response = apiService.getConversations(cursor, size)
        check(response.code == 200 && response.data != null) { response.message }
        val data = response.data
        data.items.map { it.toDomain() } to data.nextCursor
    }

    /// 获取会话历史消息
    suspend fun fetchMessages(conversationId: String): Result<List<ChatMessage>> = runCatching {
        val response = apiService.getConversationMessages(conversationId)
        check(response.code == 200 && response.data != null) { response.message }
        response.data.map { it.toDomain(conversationId) }
    }

    /// 删除会话
    suspend fun deleteConversation(conversationId: String): Result<Unit> = runCatching {
        val response = apiService.deleteConversation(conversationId)
        check(response.code == 200) { response.message }
    }

    /// 删除指定消息
    suspend fun deleteMessage(conversationId: String, messageId: Long): Result<Unit> = runCatching {
        val response = apiService.deleteConversationMessage(conversationId, messageId)
        check(response.code == 200) { response.message }
    }

    /// 清空会话消息
    suspend fun clearMessages(conversationId: String): Result<Unit> = runCatching {
        val response = apiService.clearConversationMessages(conversationId)
        check(response.code == 200) { response.message }
    }
}
