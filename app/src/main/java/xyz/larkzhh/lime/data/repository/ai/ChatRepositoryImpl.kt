package xyz.larkzhh.lime.data.repository.ai

import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import xyz.larkzhh.lime.data.local.ai.ChatLocalDataSource
import xyz.larkzhh.lime.data.mapper.toDomain
import xyz.larkzhh.lime.data.mapper.toEntity
import xyz.larkzhh.lime.data.mapper.toJson
import xyz.larkzhh.lime.data.network.ai.ChatRemoteDataSource
import xyz.larkzhh.lime.domain.model.AiModelInfo
import xyz.larkzhh.lime.domain.model.ChatConversation
import xyz.larkzhh.lime.domain.model.ChatMessage
import xyz.larkzhh.lime.domain.model.ChatMessageStatus
import xyz.larkzhh.lime.domain.model.ChatRole
import xyz.larkzhh.lime.domain.model.ChatStreamEvent
import xyz.larkzhh.lime.domain.repository.ChatRepository
import javax.inject.Inject
import javax.inject.Singleton


@OptIn(ExperimentalPagingApi::class)
@Singleton
class ChatRepositoryImpl @Inject constructor(
    private val remote: ChatRemoteDataSource,
    private val local: ChatLocalDataSource,
) : ChatRepository {

    /// AI 聊天 SSE 流
    override fun chatStream(
        conversationId: String,
        messageClientId: String,
        message: String,
        imageUrls: List<String>?,
        noteId: Long?,
        model: String?,
        search: Boolean?,
    ): Flow<ChatStreamEvent> =
        remote.chatStream(conversationId, messageClientId, message, imageUrls, noteId, model, search)

    /// 拉取模型
    override suspend fun fetchModels(): Result<List<AiModelInfo>> = remote.fetchModels()

    /// 打断正在生成的回复
    override suspend fun cancelGeneration(messageClientId: String, partialContent: String?): Result<Unit> =
        remote.cancelGeneration(messageClientId, partialContent)

    /// 远端历史消息合并进本地缓存
    override suspend fun syncMessages(conversationId: String): Result<Boolean> = runCatching {
        // 按时间升序处理远端消息
        val remoteMessages = remote.fetchMessages(conversationId).getOrThrow()
            .sortedWith(compareBy<ChatMessage> { it.createTime }.thenBy { it.serverId ?: Long.MAX_VALUE })
        val localMessages = local.getMessages(conversationId)
        var anchorLocalId: Long? = null

        remoteMessages.forEach { r ->
            val serverId = r.serverId ?: return@forEach
            val remoteRole = if (r.role == ChatRole.USER) "user" else "assistant"

            //  已同步消息
            val byServer = localMessages.firstOrNull { it.serverId == serverId }
            if (byServer != null) {
                if (byServer.content != r.content || byServer.status != r.status.name) {
                    local.updateMessageByServerId(serverId, r.content, mergeMessageStatus(byServer.status, r.status).name)
                }
                anchorLocalId = byServer.localId
                return@forEach
            }

            // 本地临时消息
            val byClient = if (r.clientId != null) {
                localMessages.lastOrNull {
                    it.serverId == null && it.clientId == r.clientId && it.role == remoteRole
                }
            } else null
            if (byClient != null) {
                local.updateMessageFull(
                    byClient.localId, serverId, r.content, r.images.toJson(),
                    mergeMessageStatus(byClient.status, r.status).name,
                )
                anchorLocalId = byClient.localId
                return@forEach
            }

            if (remoteRole == "assistant" && anchorLocalId != null) {
                val anchorIndex = localMessages.indexOfFirst { it.localId == anchorLocalId }
                val placeholder = localMessages.getOrNull(anchorIndex + 1)
                if (placeholder != null && placeholder.role == "assistant" && placeholder.serverId == null &&
                    (placeholder.status == ChatMessageStatus.STOPPED.name ||
                        placeholder.status == ChatMessageStatus.FAILED.name ||
                        placeholder.status == ChatMessageStatus.STREAMING.name)
                ) {
                    local.updateMessageFull(
                        placeholder.localId, serverId, r.content, r.images.toJson(),
                        mergeMessageStatus(placeholder.status, r.status).name,
                    )
                    anchorLocalId = placeholder.localId
                    return@forEach
                }
            }

            // 新远端消息
            local.upsertMessage(r.toEntity())
            anchorLocalId = null
        }

        local.getMessages(conversationId).forEach { entity ->
            if (entity.serverId != null) return@forEach
            if (entity.role == "assistant" &&
                (entity.status == ChatMessageStatus.FAILED.name ||
                    entity.status == ChatMessageStatus.STREAMING.name)
            ) {
                local.deleteMessage(entity.localId)
            }
        }
        // 是否仍有流式消息
        remoteMessages.any { it.status == ChatMessageStatus.STREAMING }
    }

    /// 删除远端会话、消息
    override suspend fun deleteConversationRemote(conversationId: String): Result<Unit> =
        remote.deleteConversation(conversationId)

    override suspend fun deleteMessageRemote(conversationId: String, messageId: Long): Result<Unit> =
        remote.deleteMessage(conversationId, messageId)

    override suspend fun clearMessagesRemote(conversationId: String): Result<Unit> =
        remote.clearMessages(conversationId)


    ///
    private val conversationsPager by lazy {
        Pager(
            config = PagingConfig(
                pageSize = CONVERSATION_PAGE_SIZE,
                initialLoadSize = CONVERSATION_PAGE_SIZE * 2,
                enablePlaceholders = false,
            ),
            remoteMediator = ChatConversationsRemoteMediator(remote, local),
            pagingSourceFactory = { local.conversationsPagingSource() },
        )
    }

    /// 会话列表分页流
    override fun conversationsPager(): Flow<PagingData<ChatConversation>> =
        conversationsPager.flow.map { pagingData -> pagingData.map { it.toDomain() } }

    /// 观察实时信息
    override fun observeLocalMessages(conversationId: String): Flow<List<ChatMessage>> =
        local.observeMessages(conversationId).map { list -> list.map { it.toDomain() } }

    /// 读取本地会话
    override suspend fun getLocalConversation(conversationId: String): ChatConversation? =
        local.getConversation(conversationId)?.toDomain()

    /// 最近更新会话
    override suspend fun getLatestConversation(): ChatConversation? =
        local.getLatestConversation()?.toDomain()

    /// 写入会话、消息
    override suspend fun saveMessage(message: ChatMessage): Long =
        local.upsertMessage(message.toEntity())

    override suspend fun saveMessages(messages: List<ChatMessage>) {
        local.upsertMessages(messages.map { it.toEntity() })
    }

    override suspend fun saveConversation(conversation: ChatConversation) {
        local.upsertConversation(conversation.toEntity())
    }

    /// 更新消息
    override suspend fun updateMessageStatus(localId: Long, status: ChatMessageStatus) {
        local.updateStatus(localId, status.name)
    }

    override suspend fun updateMessage(
        localId: Long,
        serverId: Long?,
        content: String,
        status: ChatMessageStatus,
    ) {
        local.updateMessage(localId, serverId, content, status.name)
    }

    override suspend fun updateMessageWithImages(
        localId: Long,
        serverId: Long?,
        content: String,
        images: List<String>?,
        status: ChatMessageStatus,
    ) {
        local.updateMessageFull(localId, serverId, content, images?.toJson(), status.name)
    }

    /// 删除本地会话、消息
    override suspend fun deleteLocalConversation(conversationId: String) {
        local.deleteConversation(conversationId)
    }

    override suspend fun deleteLocalMessage(localId: Long) {
        local.deleteMessage(localId)
    }

    override suspend fun clearLocalMessages(conversationId: String) {
        local.clearMessages(conversationId)
    }

    /// 标记失败
    override suspend fun resetStaleMessages() {
        local.resetStaleMessages()
    }

    private companion object {
        const val CONVERSATION_PAGE_SIZE = 20// 每页加载20条

        /// 合并本地与远端状态
        fun mergeMessageStatus(localStatus: String, remoteStatus: ChatMessageStatus): ChatMessageStatus {
            val local = runCatching { ChatMessageStatus.valueOf(localStatus) }
                .getOrDefault(ChatMessageStatus.DONE)
            return when {
                local == ChatMessageStatus.DONE || remoteStatus == ChatMessageStatus.DONE ->
                    ChatMessageStatus.DONE
                local == ChatMessageStatus.STOPPED -> ChatMessageStatus.STOPPED
                else -> remoteStatus
            }
        }
    }
}
