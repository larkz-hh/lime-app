package xyz.larkzhh.lime.data.network.ai

import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.HTTP
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import xyz.larkzhh.lime.data.network.model.AiChatCancelRequest
import xyz.larkzhh.lime.data.network.model.AiModelDto
import xyz.larkzhh.lime.data.network.model.AiTranslateRequest
import xyz.larkzhh.lime.data.network.model.AiTranslateResponse
import xyz.larkzhh.lime.data.network.model.ApiResponse
import xyz.larkzhh.lime.data.network.model.ChatMessageDto
import xyz.larkzhh.lime.data.network.model.ConversationListData

interface AiApi {

    /// AI 翻译
    @POST("api/ai/translate")
    suspend fun aiTranslate(@Body request: AiTranslateRequest): ApiResponse<AiTranslateResponse>

    /// AI 模型列表
    @GET("api/ai/models")
    suspend fun getAiModels(): ApiResponse<List<AiModelDto>>

    /// 打断正在生成的回复
    @POST("api/ai/chat/cancel")
    suspend fun cancelAiGeneration(@Body request: AiChatCancelRequest): ApiResponse<Unit>

    /// AI 会话列表
    @GET("api/ai/conversations")
    suspend fun getConversations(
        @Query("cursor") cursor: String?,
        @Query("size") size: Int,
    ): ApiResponse<ConversationListData>

    /// 会话历史消息
    @GET("api/ai/conversations/{conversationId}/messages")
    suspend fun getConversationMessages(
        @Path("conversationId") conversationId: String,
    ): ApiResponse<List<ChatMessageDto>>

    /// 删除会话
    @DELETE("api/ai/conversations/{conversationId}")
    suspend fun deleteConversation(@Path("conversationId") conversationId: String): ApiResponse<Unit>

    /// 删除会话中的单条消息
    @DELETE("api/ai/conversations/{conversationId}/messages/{messageId}")
    suspend fun deleteConversationMessage(
        @Path("conversationId") conversationId: String,
        @Path("messageId") messageId: Long,
    ): ApiResponse<Unit>

    /// 清空会话消息
    @HTTP(method = "DELETE", path = "api/ai/conversations/{conversationId}/messages", hasBody = false)
    suspend fun clearConversationMessages(
        @Path("conversationId") conversationId: String,
    ): ApiResponse<Unit>
}
