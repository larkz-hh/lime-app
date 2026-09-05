package xyz.larkzhh.lime.domain.repository

import kotlinx.coroutines.flow.Flow
import xyz.larkzhh.lime.domain.model.ImConversation
import xyz.larkzhh.lime.domain.model.ImMessage

/**
 * IM 仓库接口
 */
interface ImRepository {

    /// 拉取 UserSig 并登录
    suspend fun ensureImLogin(): Result<Unit>

    /// 打开私信会话
    suspend fun openConversation(targetUserId: Long): Result<String>

    /// IM 登出
    suspend fun logout()

    /// 当前 IM 登录用户 ID
    fun getLoginUser(): String?

    /// 收到的新消息流
    val newMessages: Flow<ImMessage>

    /// 话列表变化流
    val conversationChanges: Flow<List<ImConversation>>

    /// 对方撤回消息的 msgID 流
    val revokedMessages: Flow<String>

    /// 拉取会话列表
    suspend fun getConversations(): List<ImConversation>

    /// 撤回一条消息
    suspend fun revokeMessage(msgId: String): Result<Unit>

    /// 删除一条消息
    suspend fun deleteMessage(msgId: String): Result<Unit>

    /// 清空与某用户的单聊历史消息
    suspend fun clearHistory(conversationId: String): Result<Unit>

    /// 删除会话，清空本地消息记录
    suspend fun deleteConversation(conversationId: String): Result<Unit>

    /// 拉取单聊历史消息
    suspend fun getHistoryMessages(conversationId: String): List<ImMessage>

    /// 下载图片消息到本地缓存
    suspend fun downloadImage(msgId: String): String?

    /// 将会话标记为已读
    suspend fun markRead(conversationId: String): Result<Unit>

    /// 发送文本消息
    suspend fun sendText(conversationId: String, text: String): Result<ImMessage>

    /// 发送图片消息
    suspend fun sendImage(conversationId: String, imagePath: String): Result<ImMessage>
}
