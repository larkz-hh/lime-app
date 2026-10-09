package xyz.larkzhh.lime.ui.im.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import xyz.larkzhh.lime.feature.im.R
import xyz.larkzhh.lime.domain.model.ImMessage
import xyz.larkzhh.lime.domain.model.ImUserProfile
import xyz.larkzhh.lime.domain.repository.ImRepository
import xyz.larkzhh.lime.domain.repository.UserRepository
import java.io.File
import javax.inject.Inject

data class ImChatUiState(
    val messages: List<ImMessage> = emptyList(),
    val selfUserId: Long? = null,
    val selfAvatar: String? = null,
    val peerUserId: Long? = null,
    val peerAvatar: String? = null,
    val peerNickname: String? = null,
    val isGroup: Boolean = false,
    val groupId: String? = null,
    val memberProfiles: Map<String, ImUserProfile> = emptyMap(),// 群成员 IM 资料
    val isSending: Boolean = false,
    val errorMessage: String? = null,
)

/**
 * 聊天页 ViewModel
 */
@HiltViewModel
class ImChatViewModel @Inject constructor(
    private val imRepository: ImRepository,
    private val userRepository: UserRepository,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _state = MutableStateFlow(ImChatUiState())
    val state: StateFlow<ImChatUiState> = _state.asStateFlow()

    private var conversationId: String = ""

    /// 正在下载本地图片的消息 id
    private val downloadingImages = mutableSetOf<String>()

    init {
        // 当前登录用户资料
        viewModelScope.launch {
            userRepository.userFlow.collect { user ->
                _state.update { it.copy(selfUserId = user?.id, selfAvatar = user?.avatar) }
            }
        }
        // 接收新消息，仅保留当前会话的消息
        viewModelScope.launch {
            imRepository.newMessages.collect { msg ->
                if (conversationId.isEmpty()) return@collect
                val st = _state.value
                val matches = if (st.isGroup) {
                    st.groupId != null && msg.groupId == st.groupId
                } else {
                    msg.senderId == conversationId.removePrefix("c2c_")
                }
                if (!matches) return@collect
                _state.update { it.copy(messages = listOf(msg) + it.messages) }
                // 标记已读
                imRepository.markRead(conversationId)
                // 图片消息下载到本地
                ensureImageLocal(msg)
                // 群消息补充发送者资料
                if (st.isGroup) {
                    viewModelScope.launch { resolveMemberProfiles(listOf(msg)) }
                }
            }
        }
        // 标记对方撤回消息
        viewModelScope.launch {
            imRepository.revokedMessages.collect { msgId ->
                _state.update { st ->
                    st.copy(messages = st.messages.map {
                        if (it.id == msgId) it.copy(isRevoked = true) else it
                    })
                }
            }
        }
    }

    /// 撤回一条消息
    fun revokeMessage(message: ImMessage) {
        viewModelScope.launch {
            _state.update { it.copy(errorMessage = null) }
            imRepository.revokeMessage(message.id).fold(
                onSuccess = {
                    _state.update { st ->
                        st.copy(messages = st.messages.map {
                            if (it.id == message.id) it.copy(isRevoked = true) else it
                        })
                    }
                },
                onFailure = { e ->
                    _state.update { it.copy(errorMessage = e.message ?: context.getString(R.string.im_revoke_failed)) }
                },
            )
        }
    }

    /// 删除一条消息
    fun deleteMessage(message: ImMessage) {
        viewModelScope.launch {
            _state.update { it.copy(errorMessage = null) }
            imRepository.deleteMessage(message.id).fold(
                onSuccess = {
                    _state.update { st ->
                        st.copy(messages = st.messages.filterNot { it.id == message.id })
                    }
                },
                onFailure = { e ->
                    _state.update { it.copy(errorMessage = e.message ?: context.getString(R.string.im_delete_failed)) }
                },
            )
        }
    }

    /// 清空当前会话历史消息
    fun clearHistory() {
        if (conversationId.isEmpty()) return
        viewModelScope.launch {
            _state.update { it.copy(errorMessage = null) }
            imRepository.clearHistory(conversationId).fold(
                onSuccess = { _state.update { it.copy(messages = emptyList()) } },
                onFailure = { e ->
                    _state.update { it.copy(errorMessage = e.message ?: context.getString(R.string.im_clear_failed)) }
                },
            )
        }
    }

    /// 进入会话时加载历史消息
    fun load(conversationId: String) {
        val isGroup = conversationId.startsWith("group_")
        val groupId = if (isGroup) conversationId.removePrefix("group_") else null
        val isNewConversation = this.conversationId != conversationId
        if (isNewConversation) {
            this.conversationId = conversationId
            _state.update { it.copy(isGroup = isGroup, groupId = groupId, memberProfiles = emptyMap()) }
            viewModelScope.launch {
                imRepository.ensureImLogin().fold(
                    onSuccess = {
                        val history = imRepository.getHistoryMessages(conversationId)
                        _state.update { it.copy(messages = history) }
                        // 历史图片下载到本地
                        history.forEach { ensureImageLocal(it) }
                        // 群消息补充发送者资料
                        if (isGroup) {
                            resolveMemberProfiles(history)
                        }
                    },
                    onFailure = { e ->
                        _state.update { it.copy(errorMessage = e.message ?: context.getString(R.string.im_login_failed)) }
                    },
                )
            }
        }
        // 刷新资料
        if (groupId != null) {
            loadGroupProfile(groupId)
        } else {
            loadPeerProfile(conversationId)
        }
        // 清空未读
        viewModelScope.launch {
            runCatching { imRepository.markRead(conversationId) }
        }
    }

    /// 图片消息本地化
    private fun ensureImageLocal(msg: ImMessage) {
        if (!msg.isImage) return
        if (msg.isRevoked) return
        // 已有本地文件跳过
        val local = msg.imagePath?.let { File(it) }
        if (local != null && local.exists()) return
        if (downloadingImages.contains(msg.id)) return
        downloadingImages.add(msg.id)
        viewModelScope.launch {
            val path = imRepository.downloadImage(msg.id)
            downloadingImages.remove(msg.id)
            if (path != null) {
                _state.update { st ->
                    st.copy(messages = st.messages.map {
                        if (it.id == msg.id) it.copy(imagePath = path, imageUrl = null) else it
                    })
                }
            }
        }
    }

    /// 从会话 id 解析对方业务用户 id
    private fun parsePeerUserId(conversationId: String): Long? =
        conversationId.removePrefix("c2c_")
            .removePrefix("lime_")
            .toLongOrNull()

    /// 加载资料
    private fun loadPeerProfile(conversationId: String) {
        val peerId = parsePeerUserId(conversationId) ?: return
        _state.update { it.copy(peerUserId = peerId) }
        viewModelScope.launch {
            // 显示缓存资料
            userRepository.getCachedUserById(peerId)?.let { peer ->
                _state.update { it.copy(peerAvatar = peer.avatar, peerNickname = peer.nickname) }
            }
            // 再拉最新
            userRepository.getUserById(peerId).onSuccess { peer ->
                _state.update { it.copy(peerAvatar = peer.avatar, peerNickname = peer.nickname) }
            }
        }
    }

    /// 加载群资料
    private fun loadGroupProfile(groupId: String) {
        viewModelScope.launch {
            imRepository.getGroupsInfo(listOf(groupId)).firstOrNull()?.let { g ->
                _state.update { it.copy(peerNickname = g.name, peerAvatar = g.faceUrl) }
            }
        }
    }

    /// 批量补充群消息发送者的昵称、头像
    private suspend fun resolveMemberProfiles(messages: List<ImMessage>) {
        val selfId = _state.value.selfUserId?.let { "lime_$it" }
        val known = _state.value.memberProfiles.keys
        val ids = messages.asSequence()
            .map { it.senderId }
            .filter { it.isNotBlank() && it != selfId && it !in known }
            .distinct()
            .toList()
        if (ids.isEmpty()) return
        var merged = runCatching { imRepository.getUserInfos(ids) }.getOrDefault(emptyMap())
        // IM 资料缺失或昵称为空的发送者：用业务端用户昵称/头像兜底（与单聊同源）
        val unresolved = ids.filter { sid ->
            val p = merged[sid]
            p == null || p.nickname.isNullOrBlank()
        }
        if (unresolved.isNotEmpty()) {
            for (sid in unresolved) {
                val uid = sid.removePrefix("lime_").toLongOrNull() ?: continue
                val user = userRepository.getCachedUserById(uid)
                    ?: userRepository.getUserById(uid).getOrNull()
                    ?: continue
                merged = merged + (sid to ImUserProfile(user.nickname, user.avatar))
            }
        }
        if (merged.isNotEmpty()) {
            _state.update { it.copy(memberProfiles = it.memberProfiles + merged) }
        }
    }

    /// 发送文本消息
    fun sendText(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty() || conversationId.isEmpty()) return
        viewModelScope.launch {
            _state.update { it.copy(isSending = true, errorMessage = null) }
            imRepository.sendText(conversationId, trimmed).fold(
                onSuccess = { sent ->
                    _state.update { it.copy(isSending = false, messages = listOf(sent) + it.messages) }
                },
                onFailure = { e ->
                    _state.update { it.copy(isSending = false, errorMessage = e.message ?: context.getString(R.string.im_send_failed)) }
                },
            )
        }
    }

    /// 发送图片消息
    fun sendImage(imagePath: String) {
        if (imagePath.isEmpty() || conversationId.isEmpty()) return
        viewModelScope.launch {
            _state.update { it.copy(isSending = true, errorMessage = null) }
            imRepository.sendImage(conversationId, imagePath).fold(
                onSuccess = { sent ->
                    _state.update { it.copy(isSending = false, messages = listOf(sent) + it.messages) }
                },
                onFailure = { e ->
                    _state.update { it.copy(isSending = false, errorMessage = e.message ?: context.getString(R.string.im_send_failed)) }
                },
            )
        }
    }

    fun clearError() = _state.update { it.copy(errorMessage = null) }
}
