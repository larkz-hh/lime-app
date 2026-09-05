package xyz.larkzhh.lime.ui.im.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import xyz.larkzhh.lime.domain.model.ImMessage
import xyz.larkzhh.lime.domain.repository.ImRepository
import xyz.larkzhh.lime.domain.repository.UserRepository
import javax.inject.Inject


data class ImChatUiState(
    val messages: List<ImMessage> = emptyList(),
    val selfUserId: Long? = null,
    val selfAvatar: String? = null,
    val peerUserId: Long? = null,
    val peerAvatar: String? = null,
    val peerNickname: String? = null,
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
) : ViewModel() {

    private val _state = MutableStateFlow(ImChatUiState())
    val state: StateFlow<ImChatUiState> = _state.asStateFlow()

    private var conversationId: String = ""

    init {
        // 当前登录用户资料
        viewModelScope.launch {
            userRepository.userFlow.collect { user ->
                _state.update { it.copy(selfUserId = user?.id, selfAvatar = user?.avatar) }
            }
        }
        // 接收新消息，仅保留当前会话对方发来的消息
        viewModelScope.launch {
            imRepository.newMessages.collect { msg ->
                val peer = conversationId.removePrefix("c2c_")
                if (conversationId.isNotEmpty() && msg.senderId == peer) {
                    _state.update { it.copy(messages = listOf(msg) + it.messages) }
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
                    _state.update { it.copy(errorMessage = e.message ?: "撤回失败") }
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
                    _state.update { it.copy(errorMessage = e.message ?: "删除失败") }
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
                    _state.update { it.copy(errorMessage = e.message ?: "清空失败") }
                },
            )
        }
    }

    /// 进入会话时加载历史消息
    fun load(conversationId: String) {
        if (this.conversationId == conversationId) return
        this.conversationId = conversationId
        viewModelScope.launch {
            imRepository.ensureImLogin().fold(
                onSuccess = {
                    val history = imRepository.getHistoryMessages(conversationId)
                    _state.update { it.copy(messages = history) }
                },
                onFailure = { e ->
                    _state.update { it.copy(errorMessage = e.message ?: "IM 登录失败") }
                },
            )
        }
        loadPeerProfile(conversationId)
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
                    _state.update { it.copy(isSending = false, errorMessage = e.message ?: "发送失败") }
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
                    _state.update { it.copy(isSending = false, errorMessage = e.message ?: "发送失败") }
                },
            )
        }
    }

    fun clearError() = _state.update { it.copy(errorMessage = null) }
}
