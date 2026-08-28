package xyz.larkzhh.lime.data.repository.chat

import androidx.paging.PagingSource
import kotlinx.coroutines.flow.Flow
import xyz.larkzhh.lime.data.local.chat.ChatDao
import xyz.larkzhh.lime.data.local.chat.ConversationEntity
import xyz.larkzhh.lime.data.local.chat.MessageEntity
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 消息本地数据源
 */
@Singleton
class ChatLocalDataSource @Inject constructor(
    private val chatDao: ChatDao,
) {

    /// 分页数据源
    fun conversationsPagingSource(): PagingSource<Int, ConversationEntity> =
        chatDao.conversationsPagingSource()

    /// 查单个会话
    suspend fun getConversation(id: Long): ConversationEntity? = chatDao.getConversation(id)

    /// 查最新会话
    suspend fun getLatestConversation(): ConversationEntity? = chatDao.getLatestConversation()

    /// /// 插入或更新会话
    suspend fun upsertConversation(conversation: ConversationEntity) =
        chatDao.upsertConversation(conversation)

    /// 首屏刷新清空旧缓存并写入新一页
    suspend fun replaceConversations(conversations: List<ConversationEntity>) {
        chatDao.deleteAllConversations()
        chatDao.upsertConversations(conversations)
    }

    suspend fun upsertConversations(conversations: List<ConversationEntity>) =
        chatDao.upsertConversations(conversations)

    /// 删除会话与相关消息
    suspend fun deleteConversation(id: Long) {
        chatDao.deleteConversationWithMessages(id)
    }

    /// 观察实时消息
    fun observeMessages(conversationId: Long): Flow<List<MessageEntity>> =
        chatDao.observeMessages(conversationId)

    /// 查询消息列表
    suspend fun getMessages(conversationId: Long): List<MessageEntity> =
        chatDao.getMessages(conversationId)

    /// 插入并返回消息 id
    suspend fun upsertMessage(message: MessageEntity): Long = chatDao.upsertMessage(message)

    suspend fun upsertMessages(messages: List<MessageEntity>) = chatDao.upsertMessages(messages)

    /// 修改状态
    suspend fun updateStatus(localId: Long, status: String) = chatDao.updateStatus(localId, status)

    /// 修改消息
    suspend fun updateMessage(localId: Long, serverId: Long?, content: String, status: String) =
        chatDao.updateMessage(localId, serverId, content, status)

    suspend fun updateMessageFull(
        localId: Long,
        serverId: Long?,
        content: String,
        images: String?,
        status: String,
    ) = chatDao.updateMessageFull(localId, serverId, content, images, status)

    /// 批量迁移会话 id
    suspend fun moveMessagesToConversation(oldId: Long, newId: Long) =
        chatDao.moveMessagesToConversation(oldId, newId)

    /// 删除消息
    suspend fun deleteMessage(localId: Long) = chatDao.deleteMessage(localId)

    suspend fun clearMessages(conversationId: Long) = chatDao.clearMessages(conversationId)

    /// 清理进行中消息
    suspend fun resetStaleMessages() = chatDao.resetStaleMessages()

}
