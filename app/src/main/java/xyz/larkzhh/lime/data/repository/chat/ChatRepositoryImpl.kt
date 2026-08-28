package xyz.larkzhh.lime.data.repository.chat

import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import xyz.larkzhh.lime.domain.model.AiModelInfo
import xyz.larkzhh.lime.domain.model.ChatConversation
import xyz.larkzhh.lime.domain.model.ChatMessage
import xyz.larkzhh.lime.domain.model.ChatMessageStatus
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
        conversationId: Long?,
        message: String,
        imageUrls: List<String>?,
        model: String?,
    ): Flow<ChatStreamEvent> = remote.chatStream(conversationId, message, imageUrls, model)

    /// 拉取模型
    override suspend fun fetchModels(): Result<List<AiModelInfo>> = remote.fetchModels()

    /// 远端历史消息合并进本地缓存
    override suspend fun syncMessages(conversationId: Long): Result<Unit> = runCatching {
        val remoteMessages = remote.fetchMessages(conversationId).getOrThrow()
        val localMessages = local.getMessages(conversationId)
        // 增量合并
        val missing = remoteMessages.filter { r ->
            localMessages.none { it.serverId == r.serverId }
        }
        if (missing.isNotEmpty()) {
            local.upsertMessages(missing.map { it.toEntity() })
        }
    }

    /// 删除远端会话、消息
    override suspend fun deleteConversationRemote(conversationId: Long): Result<Unit> =
        remote.deleteConversation(conversationId)

    override suspend fun deleteMessageRemote(conversationId: Long, messageId: Long): Result<Unit> =
        remote.deleteMessage(conversationId, messageId)

    override suspend fun clearMessagesRemote(conversationId: Long): Result<Unit> =
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
    override fun observeLocalMessages(conversationId: Long): Flow<List<ChatMessage>> =
        local.observeMessages(conversationId).map { list -> list.map { it.toDomain() } }

    /// 读取本地会话
    override suspend fun getLocalConversation(conversationId: Long): ChatConversation? =
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

    /// 临时会话迁移
    override suspend fun moveMessagesToConversation(oldId: Long, newId: Long) {
        local.moveMessagesToConversation(oldId, newId)
    }

    /// 删除本地会话、消息
    override suspend fun deleteLocalConversation(conversationId: Long) {
        local.deleteConversation(conversationId)
    }

    override suspend fun deleteLocalMessage(localId: Long) {
        local.deleteMessage(localId)
    }

    override suspend fun clearLocalMessages(conversationId: Long) {
        local.clearMessages(conversationId)
    }

    /// 标记失败
    override suspend fun resetStaleMessages() {
        local.resetStaleMessages()
    }

    private companion object {
        const val CONVERSATION_PAGE_SIZE = 20// 每页加载20条
    }
}
