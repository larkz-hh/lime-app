package xyz.larkzhh.lime.data.local.ai

import androidx.paging.PagingSource
import kotlinx.coroutines.flow.Flow
import xyz.larkzhh.lime.data.local.UserDatabases
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 消息本地数据源
 */
@Singleton
class ChatLocalDataSource @Inject constructor(
    private val userDatabases: UserDatabases,
) {

    private fun dao(): ChatDao = userDatabases.chatDao()

    /// 分页数据源
    fun conversationsPagingSource(): PagingSource<Int, ConversationEntity> =
        dao().conversationsPagingSource()

    /// 查单个会话
    suspend fun getConversation(id: String): ConversationEntity? = dao().getConversation(id)

    /// 查最新会话
    suspend fun getLatestConversation(): ConversationEntity? = dao().getLatestConversation()

    /// 插入或更新会话
    suspend fun upsertConversation(conversation: ConversationEntity) =
        dao().upsertConversation(conversation)

    /// 首屏刷新清空旧缓存并写入新一页
    suspend fun replaceConversations(conversations: List<ConversationEntity>) {
        dao().deleteAllConversations()
        dao().upsertConversations(conversations)
    }

    suspend fun upsertConversations(conversations: List<ConversationEntity>) =
        dao().upsertConversations(conversations)

    /// 删除会话与相关消息
    suspend fun deleteConversation(id: String) {
        dao().deleteConversationWithMessages(id)
    }

    /// 观察实时消息
    fun observeMessages(conversationId: String): Flow<List<MessageEntity>> =
        dao().observeMessages(conversationId)

    /// 查询消息列表
    suspend fun getMessages(conversationId: String): List<MessageEntity> =
        dao().getMessages(conversationId)

    /// 插入并返回消息 id
    suspend fun upsertMessage(message: MessageEntity): Long = dao().upsertMessage(message)

    suspend fun upsertMessages(messages: List<MessageEntity>) = dao().upsertMessages(messages)

    /// 修改状态
    suspend fun updateStatus(localId: Long, status: String) = dao().updateStatus(localId, status)

    /// 修改消息
    suspend fun updateMessage(localId: Long, serverId: Long?, content: String, status: String) =
        dao().updateMessage(localId, serverId, content, status)

    suspend fun updateMessageFull(
        localId: Long,
        serverId: Long?,
        content: String,
        images: String?,
        status: String,
    ) = dao().updateMessageFull(localId, serverId, content, images, status)

    /// 按服务端消息 id 更新内容与状态
    suspend fun updateMessageByServerId(serverId: Long, content: String, status: String) =
        dao().updateMessageByServerId(serverId, content, status)

    /// 删除消息
    suspend fun deleteMessage(localId: Long) = dao().deleteMessage(localId)

    suspend fun clearMessages(conversationId: String) = dao().clearMessages(conversationId)

    /// 清理进行中消息
    suspend fun resetStaleMessages() = dao().resetStaleMessages()
}
