package xyz.larkzhh.lime.domain.repository

import kotlinx.coroutines.flow.Flow
import xyz.larkzhh.lime.domain.model.ImConversation
import xyz.larkzhh.lime.domain.model.ImGroup
import xyz.larkzhh.lime.domain.model.ImMessage
import xyz.larkzhh.lime.domain.model.ImUserProfile

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

    /// IM 会话未读数合计
    val conversationUnreadFlow: Flow<Int>

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

    /// 创建 Work 群
    suspend fun createGroup(
        name: String,
        introduction: String?,
        initialMemberIds: List<String>,
    ): Result<String>

    /// 拉取我已加入的群列表
    suspend fun getJoinedGroups(): List<ImGroup>

    /// 拉取指定群资料
    suspend fun getGroupsInfo(groupIds: List<String>): List<ImGroup>

    /// 修改群资料
    suspend fun updateGroupInfo(
        groupId: String,
        name: String? = null,
        introduction: String? = null,
        faceUrl: String? = null,
    ): Result<Unit>

    /// 邀请成员入群
    suspend fun inviteToGroup(groupId: String, userIds: List<String>): Result<Unit>

    /// 退出群聊
    suspend fun quitGroup(groupId: String): Result<Unit>

    /// 解散群聊
    suspend fun dismissGroup(groupId: String): Result<Unit>

    /// 当前用户在群里的角色，400=群主 300=管理员 200=普通成员
    suspend fun getSelfRole(groupId: String): Result<Int>

    /// 批量拉取 IM 用户资料
    suspend fun getUserInfos(userIds: List<String>): Map<String, ImUserProfile>
}
