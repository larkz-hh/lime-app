package xyz.larkzhh.lime.data.local.chat

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/**
 * AI 聊天本地缓存 DAO
 */
@Dao
interface ChatDao {

    /// 分页数据源
    @Query("SELECT * FROM ai_conversations ORDER BY updateTime DESC")
    fun conversationsPagingSource(): PagingSource<Int, ConversationEntity>

    /// 查单个会话
    @Query("SELECT * FROM ai_conversations WHERE id = :id")
    suspend fun getConversation(id: Long): ConversationEntity?

    /// 查最新会话
    @Query("SELECT * FROM ai_conversations ORDER BY updateTime DESC LIMIT 1")
    suspend fun getLatestConversation(): ConversationEntity?

    /// 插入或更新会话
    @Upsert
    suspend fun upsertConversation(conversation: ConversationEntity)

    @Upsert
    suspend fun upsertConversations(conversations: List<ConversationEntity>)

    /// 删除会话
    @Query("DELETE FROM ai_conversations")
    suspend fun deleteAllConversations()

    @Query("DELETE FROM ai_conversations WHERE id = :id")
    suspend fun deleteConversation(id: Long)

    // 观察消息列表
    @Query("SELECT * FROM ai_messages WHERE conversationId = :conversationId ORDER BY createTime ASC, localId ASC")
    fun observeMessages(conversationId: Long): Flow<List<MessageEntity>>

    /// 查询消息列表
    @Query("SELECT * FROM ai_messages WHERE conversationId = :conversationId ORDER BY createTime ASC, localId ASC")
    suspend fun getMessages(conversationId: Long): List<MessageEntity>

    /// 插入并返回消息 id
    @Upsert
    suspend fun upsertMessage(message: MessageEntity): Long

    @Upsert
    suspend fun upsertMessages(messages: List<MessageEntity>)

    /// 修改状态
    @Query("UPDATE ai_messages SET status = :status WHERE localId = :localId")
    suspend fun updateStatus(localId: Long, status: String)

    /// 更新消息
    @Query("UPDATE ai_messages SET status = :status, content = :content, serverId = :serverId WHERE localId = :localId")
    suspend fun updateMessage(localId: Long, serverId: Long?, content: String, status: String)

    @Query("UPDATE ai_messages SET status = :status, content = :content, serverId = :serverId, images = :images WHERE localId = :localId")
    suspend fun updateMessageFull(localId: Long, serverId: Long?, content: String, images: String?, status: String)

    /// 批量迁移会话 id
    @Query("UPDATE ai_messages SET conversationId = :newId WHERE conversationId = :oldId")
    suspend fun moveMessagesToConversation(oldId: Long, newId: Long)

    /// 删除、清空消息
    @Query("DELETE FROM ai_messages WHERE localId = :localId")
    suspend fun deleteMessage(localId: Long)

    @Query("DELETE FROM ai_messages WHERE conversationId = :conversationId")
    suspend fun clearMessages(conversationId: Long)

    /// 原子删除会话与其所有消息
    @Transaction
    suspend fun deleteConversationWithMessages(id: Long) {
        deleteConversation(id)
        clearMessages(id)
    }

    /// 重置失败
    @Query("UPDATE ai_messages SET status = 'FAILED' WHERE status IN ('SENDING', 'STREAMING')")
    suspend fun resetStaleMessages()


}
