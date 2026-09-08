package xyz.larkzhh.lime.data.repository

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch
import xyz.larkzhh.lime.data.im.ImException
import xyz.larkzhh.lime.data.im.ImManager
import xyz.larkzhh.lime.data.network.im.ImApi
import xyz.larkzhh.lime.data.network.model.ConversationOpenRequest
import xyz.larkzhh.lime.domain.model.ImConversation
import xyz.larkzhh.lime.domain.model.ImGroup
import xyz.larkzhh.lime.domain.model.ImMessage
import xyz.larkzhh.lime.domain.model.ImUserProfile
import xyz.larkzhh.lime.domain.model.toImConversation
import xyz.larkzhh.lime.domain.model.toImGroup
import xyz.larkzhh.lime.domain.model.toImMessage
import xyz.larkzhh.lime.domain.repository.ImRepository
import xyz.larkzhh.lime.domain.repository.UserRepository
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * IM 仓库实现
 */
@Singleton
class ImRepositoryImpl @Inject constructor(
    private val apiService: ImApi,
    private val imManager: ImManager,
    private val userRepository: UserRepository,
) : ImRepository {

    /// 手动刷新未读数
    private val refreshTrigger = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    /// 会话最新可见消息预览缓存
    private val lastVisiblePreview = ConcurrentHashMap<String, String>()
    private val previewScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        previewScope.launch {
            newMessages.collect { msg ->
                val convId = msg.groupId?.let { "group_$it" } ?: "c2c_${msg.senderId}"
                lastVisiblePreview[convId] = msg.text ?: "[图片]"
            }
        }
    }

    /// IM 会话未读数合计
    @OptIn(ExperimentalCoroutinesApi::class)
    override val conversationUnreadFlow: Flow<Int> = merge(
        refreshTrigger,
        imManager.conversationChanges.map { },
    ).flatMapLatest {
        flow {
            runCatching { imManager.getConversationList() }
                .getOrNull()
                ?.let { list -> emit(list.sumOf { it.unreadCount }) }
        }
    }.distinctUntilChanged()

    /// 确保 IM 已登录
    override suspend fun ensureImLogin(): Result<Unit> = runCatching {
        val response = apiService.getImUserSig()// 拉取 UserSig 并登录
        check(response.code == 200 && response.data != null) { response.message }
        val data = response.data
        imManager.init(data.sdkAppId)
        // 已登录同一用户跳过
        if (imManager.getLoginUser() != data.userId) {
            imManager.login(data.userId, data.userSig)
        }
        // 同步昵称、头像到 IM 用户资料
        userRepository.userFlow.value?.let { user ->
            runCatching { imManager.updateSelfProfile(user.nickname, user.avatar) }
        }
        // 登录完成后刷新一次未读数
        refreshTrigger.tryEmit(Unit)
    }

    /// 打开私信会话
    override suspend fun openConversation(targetUserId: Long): Result<String> {
        val response = apiService.openConversation(ConversationOpenRequest(targetUserId))
        return if (response.code == 200 && response.data != null) {
            Result.success(response.data.conversationId)
        } else {
            // 403 未互关
            val message = if (response.code == 403) "需互相关注后才能私信哦" else response.message
            Result.failure(ImException(response.code, message))
        }
    }

    /// IM 登出
    override suspend fun logout() {
        runCatching { imManager.logout() }
    }

    /// 当前已登录的 IM 用户 ID
    override fun getLoginUser(): String? = imManager.getLoginUser()

    /// 收到的新消息流
    override val newMessages: Flow<ImMessage> = imManager.newMessages
        .mapNotNull { it.toImMessage(imManager.getLoginUser()) }

    /// 会话列表变化流
    override val conversationChanges: Flow<List<ImConversation>> = imManager.conversationChanges
        .map { list -> list.map { it.toImConversation() } }

    /// 对方撤回消息 msgID 流
    override val revokedMessages: Flow<String> = imManager.revokedMessages

    /// 拉取私信会话列表
    override suspend fun getConversations(): List<ImConversation> {
        val list = imManager.getConversationList().map { it.toImConversation() }
        // 群会话补全群名、群头像
        val groupIds = list.mapNotNull { c ->
            if (c.conversationId.startsWith("group_")) c.conversationId.removePrefix("group_") else null
        }
        val base = if (groupIds.isEmpty()) {
            list
        } else {
            val groupMap = runCatching {
                imManager.getGroupsInfo(groupIds).associateBy { it.groupID }
            }.getOrDefault(emptyMap())
            list.map { c ->
                if (!c.conversationId.startsWith("group_")) return@map c
                val g = groupMap[c.conversationId.removePrefix("group_")] ?: return@map c
                c.copy(
                    showName = c.showName.ifBlank { g.groupName ?: "" },
                    faceUrl = c.faceUrl ?: g.faceUrl,
                )
            }
        }
        // 预览兜底
        base.filter { it.lastMessageText.isBlank() && !lastVisiblePreview.containsKey(it.conversationId) }
            .forEach { conv ->
                runCatching { fetchLastVisiblePreview(conv.conversationId) }
                    .getOrNull()
                    ?.let { lastVisiblePreview[conv.conversationId] = it }
            }
        return base.map { conv ->
            if (conv.lastMessageText.isNotBlank()) {
                lastVisiblePreview[conv.conversationId] = conv.lastMessageText
                conv
            } else {
                lastVisiblePreview[conv.conversationId]?.let { conv.copy(lastMessageText = it) } ?: conv
            }
        }
    }

    /// 拉取某会话最近一条可见消息
    private suspend fun fetchLastVisiblePreview(conversationId: String): String? {
        val history = getHistoryMessages(conversationId)
        for (m in history.asReversed()) {
            if (m.isRevoked) continue
            m.text?.takeIf { it.isNotBlank() }?.let { return it }
            if (m.isImage) return "[图片]"
        }
        return null
    }

    /// 撤回一条消息
    override suspend fun revokeMessage(msgId: String): Result<Unit> = runCatching {
        imManager.revokeMessage(msgId)
    }

    /// 删除一条消息
    override suspend fun deleteMessage(msgId: String): Result<Unit> = runCatching {
        imManager.deleteMessage(msgId)
    }

    /// 清空与某用户的单聊历史消息
    override suspend fun clearHistory(conversationId: String): Result<Unit> = runCatching {
        if (conversationId.startsWith("group_")) {
            imManager.deleteConversation(conversationId)
        } else {
            imManager.clearC2CHistoryMessage(conversationId.removePrefix("c2c_"))
        }
    }

    /// 删除会话
    override suspend fun deleteConversation(conversationId: String): Result<Unit> = runCatching {
        imManager.deleteConversation(conversationId)
    }

    /// 拉取历史消息（
    override suspend fun getHistoryMessages(conversationId: String): List<ImMessage> {
        val self = imManager.getLoginUser()
        val messages = if (conversationId.startsWith("group_")) {
            imManager.getGroupHistoryMessageList(conversationId.removePrefix("group_"), 50)
        } else {
            imManager.getC2CHistoryMessageList(conversationId.removePrefix("c2c_"), 50)
        }
        return messages.mapNotNull { it.toImMessage(self) }
    }

    /// 下载图片消息到本地缓存
    override suspend fun downloadImage(msgId: String): String? = runCatching {
        imManager.downloadImageToCache(msgId)
    }.getOrNull()

    /// 标记会话已读
    override suspend fun markRead(conversationId: String): Result<Unit> = runCatching {
        if (conversationId.startsWith("group_")) {
            imManager.markGroupMessageAsRead(conversationId.removePrefix("group_"))
        } else {
            imManager.markC2CMessageAsRead(conversationId.removePrefix("c2c_"))
        }
    }

    /// 发送文本消息
    override suspend fun sendText(conversationId: String, text: String): Result<ImMessage> = runCatching {
        val sent = if (conversationId.startsWith("group_")) {
            imManager.sendGroupTextMessage(conversationId.removePrefix("group_"), text)
        } else {
            imManager.sendTextMessage(conversationId, text)
        }
        sent.toImMessage(imManager.getLoginUser()) ?: error("发送失败")
    }

    /// 发送图片消息
    override suspend fun sendImage(conversationId: String, imagePath: String): Result<ImMessage> = runCatching {
        val sent = if (conversationId.startsWith("group_")) {
            imManager.sendGroupImageMessage(conversationId.removePrefix("group_"), imagePath)
        } else {
            imManager.sendImageMessage(conversationId, imagePath)
        }
        sent.toImMessage(imManager.getLoginUser()) ?: error("发送失败")
    }

    /// 创建 Work 群
    override suspend fun createGroup(
        name: String,
        introduction: String?,
        initialMemberIds: List<String>,
    ): Result<String> = runCatching {
        imManager.createGroup(name, introduction, initialMemberIds)
    }

    /// 拉取我已加入的群列表
    override suspend fun getJoinedGroups(): List<ImGroup> =
        imManager.getJoinedGroupList().map { it.toImGroup() }

    /// 拉取指定群资料
    override suspend fun getGroupsInfo(groupIds: List<String>): List<ImGroup> =
        imManager.getGroupsInfo(groupIds).map { it.toImGroup() }

    /// 修改群资料
    override suspend fun updateGroupInfo(
        groupId: String,
        name: String?,
        introduction: String?,
        faceUrl: String?,
    ): Result<Unit> = runCatching {
        imManager.setGroupInfo(groupId, name, introduction, faceUrl)
    }

    /// 邀请成员入群
    override suspend fun inviteToGroup(groupId: String, userIds: List<String>): Result<Unit> = runCatching {
        imManager.inviteUserToGroup(groupId, userIds)
    }

    /// 退出群聊
    override suspend fun quitGroup(groupId: String): Result<Unit> = runCatching {
        imManager.quitGroup(groupId)
    }

    /// 解散群聊
    override suspend fun dismissGroup(groupId: String): Result<Unit> = runCatching {
        imManager.dismissGroup(groupId)
    }

    /// 当前用户在群里的角色
    override suspend fun getSelfRole(groupId: String): Result<Int> = runCatching {
        imManager.getSelfRoleInGroup(groupId)
    }

    /// 批量拉取 IM 用户资料
    override suspend fun getUserInfos(userIds: List<String>): Map<String, ImUserProfile> {
        if (userIds.isEmpty()) return emptyMap()
        return imManager.getUsersInfo(userIds.distinct())
            .associate { info ->
                info.userID to ImUserProfile(nickname = info.nickName, faceUrl = info.faceUrl)
            }
    }
}
