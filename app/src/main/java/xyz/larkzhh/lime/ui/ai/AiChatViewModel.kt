package xyz.larkzhh.lime.ui.ai

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import xyz.larkzhh.lime.data.repository.chat.ChatSendEngine
import xyz.larkzhh.lime.data.repository.chat.ChatSendState
import xyz.larkzhh.lime.domain.model.AiModelInfo
import xyz.larkzhh.lime.domain.model.ChatConversation
import xyz.larkzhh.lime.domain.model.ChatMessage
import xyz.larkzhh.lime.domain.model.ChatMessageStatus
import xyz.larkzhh.lime.domain.model.ChatRole
import xyz.larkzhh.lime.domain.repository.ChatRepository
import xyz.larkzhh.lime.navigation.Screen
import xyz.larkzhh.lime.util.NetworkMonitor
import java.util.UUID
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

/// 输入栏待发送图片
data class PendingImage(
    val localUri: String,
    val remoteUrl: String? = null,
    val state: PendingImageState = PendingImageState.PENDING,
)

enum class PendingImageState { PENDING, UPLOADING, FAILED }

data class AiChatUiState(
    val serverConversationId: String? = null,// 新会话 null
    val localConversationId: String = "",
    val title: String = "",
    val messages: List<ChatMessage> = emptyList(),
    val inputText: String = "",
    val pendingImages: List<PendingImage> = emptyList(),
    val busy: Boolean = false,
    val streaming: Boolean = false,
    val isOffline: Boolean = false,
    val error: String? = null,
    val models: List<AiModelInfo> = emptyList(),
    val selectedModel: String? = null,
    val showModelPicker: Boolean = false,
    val showClearDialog: Boolean = false,
)

/**
 * AI 聊天页 ViewModel
 */
@HiltViewModel
class AiChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val chatSendEngine: ChatSendEngine,
    networkMonitor: NetworkMonitor,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val _state = MutableStateFlow(AiChatUiState())
    val state: StateFlow<AiChatUiState> = _state.asStateFlow()

    private var sendJob: Job? = null
    private var messagesJob: Job? = null

    /// 历史会话列表
    val conversations: Flow<PagingData<ChatConversation>> =
        chatRepository.conversationsPager().cachedIn(viewModelScope)

    init {
        val enteredId =
            savedStateHandle.get<String>("conversationId") ?: Screen.AiChat.NEW_CONVERSATION

        viewModelScope.launch {
            chatRepository.resetStaleMessages()
            when {
                // 打开最近会话
                enteredId == Screen.AiChat.LATEST_CONVERSATION -> {
                    val latest = chatRepository.getLatestConversation()
                    if (latest != null) openConversation(latest.id) else resetToNewConversation()
                }

                enteredId != Screen.AiChat.NEW_CONVERSATION -> openConversation(enteredId)
                else -> resetToNewConversation()
            }
        }

        viewModelScope.launch {
            networkMonitor.isOnline.collect { online ->
                val wasOffline = _state.value.isOffline
                val isOffline = !online
                _state.update { it.copy(isOffline = isOffline) }
                // 同步补齐
                if (wasOffline && !isOffline) {
                    _state.value.serverConversationId?.let { cid ->
                        syncAndPoll(cid)
                    }
                }
            }
        }

        // 模型列表
        viewModelScope.launch {
            chatRepository.fetchModels().onSuccess { models ->
                _state.update { it.copy(models = models) }
            }
        }
    }

    /// 打开指定会话
    fun openConversation(id: String) {
        if (_state.value.serverConversationId == id) return
        sendJob?.cancel()
        _state.update {
            it.copy(
                serverConversationId = id,
                localConversationId = id,
                title = "",
                messages = emptyList(),
                inputText = "",
                pendingImages = emptyList(),
                busy = false,
                streaming = false,
                error = null,
            )
        }
        observeMessages(id)
        syncAndPoll(id)
        viewModelScope.launch {
            chatRepository.getLocalConversation(id)?.let { c ->
                _state.update { it.copy(title = c.title) }
            }
        }
    }

    /// 新建对话
    fun startNewConversation() {
        if (_state.value.serverConversationId == null) return
        resetToNewConversation()
    }

    /// 重置为空白新会话
    private fun resetToNewConversation() {
        sendJob?.cancel()
        val newId = UUID.randomUUID().toString()
        _state.update {
            it.copy(
                serverConversationId = null,
                localConversationId = newId,
                title = "新对话",
                messages = emptyList(),
                inputText = "",
                pendingImages = emptyList(),
                busy = false,
                streaming = false,
                error = null,
            )
        }
        observeMessages(newId)
    }

    /// 删除会话
    fun deleteConversation(conversation: ChatConversation, onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            chatRepository.deleteConversationRemote(conversation.id).fold(
                onSuccess = {
                    chatRepository.deleteLocalConversation(conversation.id)
                    // 删除当前会话进入回到新对话
                    if (_state.value.serverConversationId == conversation.id) {
                        startNewConversation()
                    }
                    onDone(true)
                },
                onFailure = { e ->
                    _state.update { s -> s.copy(error = e.message ?: "删除失败，请重试") }
                    onDone(false)
                },
            )
        }
    }

    fun clearMessages(conversation: ChatConversation, onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            chatRepository.clearMessagesRemote(conversation.id).fold(
                onSuccess = {
                    chatRepository.clearLocalMessages(conversation.id)
                    onDone(true)
                },
                onFailure = { e ->
                    _state.update { s -> s.copy(error = e.message ?: "清空失败，请重试") }
                    onDone(false)
                },
            )
        }
    }

    fun consumeError() {
        _state.update { it.copy(error = null) }
    }

    fun onInputChange(text: String) {
        _state.update { it.copy(inputText = text.take(MAX_MESSAGE_LENGTH)) }
    }

    fun addImages(uris: List<String>) {
        _state.update { s ->
            val existing = s.pendingImages.map { it.localUri }.toSet()
            val fresh = uris.filter { it !in existing }.take(MAX_IMAGES - s.pendingImages.size)
            s.copy(pendingImages = s.pendingImages + fresh.map { PendingImage(localUri = it) })
        }
    }

    fun removeImage(uri: String) {
        _state.update { s -> s.copy(pendingImages = s.pendingImages.filterNot { it.localUri == uri }) }
    }

    fun retryImage(uri: String) {
        _state.update { s ->
            s.copy(
                pendingImages = s.pendingImages.map {
                    if (it.localUri == uri) it.copy(state = PendingImageState.PENDING) else it
                }
            )
        }
    }

    // 发送消息
    fun send() {
        val s = _state.value
        if (s.busy || s.streaming) return
        val text = s.inputText.trim()
        if (text.isBlank() && s.pendingImages.isEmpty()) return
        if (s.isOffline) {
            _state.update { it.copy(error = "当前无网络，无法发送") }
            return
        }
        if (s.pendingImages.any { it.state == PendingImageState.FAILED }) {
            _state.update { it.copy(error = "有图片上传失败，请点击重试或移除") }
            return
        }
        val images = s.pendingImages.map { it.localUri }
        val conversationId = s.localConversationId
        val wasNew = s.serverConversationId == null
        val model = s.selectedModel

        _state.update { it.copy(inputText = "", pendingImages = emptyList()) }

        sendJob?.cancel()
        sendJob = viewModelScope.launch {
            val messageClientId = UUID.randomUUID().toString()
            // 缓存
            val userLocalId = chatRepository.saveMessage(
                ChatMessage(
                    conversationId = conversationId,
                    clientId = messageClientId,
                    role = ChatRole.USER,
                    content = text,
                    localImageUris = images,
                    status = ChatMessageStatus.SENDING,
                )
            )
            val result = chatSendEngine.send(
                conversationId = conversationId,
                userMessageLocalId = userLocalId,
                messageClientId = messageClientId,
                displayText = text,
                imageLocalUris = images,
                model = model,
                onState = ::applySendState,
            )
            result.onSuccess { cid -> onSendSuccess(wasNew, cid, text) }
        }
    }

    /// 重发失败的用户消息
    fun retryMessage(message: ChatMessage) {
        if (_state.value.busy || _state.value.streaming) return
        if (_state.value.isOffline) {
            _state.update { it.copy(error = "当前无网络，无法重发") }
            return
        }
        val conversationId = _state.value.localConversationId
        val wasNew = _state.value.serverConversationId == null
        val model = _state.value.selectedModel

        sendJob?.cancel()
        sendJob = viewModelScope.launch {
            // 清理上次失败产生的 ai 消息
            clearFailedAssistant(message.localId)
            val result = chatSendEngine.send(
                conversationId = conversationId,
                userMessageLocalId = message.localId,
                messageClientId = message.clientId ?: UUID.randomUUID().toString(),
                displayText = message.content,
                imageLocalUris = message.localImageUris,
                model = model,
                onState = ::applySendState,
            )
            result.onSuccess { cid -> onSendSuccess(wasNew, cid, message.content) }
        }
    }

    /// 停止生成，断开 SSE
    fun stop() {
        val msgs = _state.value.messages
        val clientId = msgs.lastOrNull {
            it.role == ChatRole.USER && it.status == ChatMessageStatus.SENDING
        }?.clientId
        val partial = msgs.lastOrNull {
            it.role == ChatRole.ASSISTANT && it.status == ChatMessageStatus.STREAMING
        }?.content
        sendJob?.cancel()
        sendJob = null
        _state.update { it.copy(streaming = false, busy = false) }
        if (clientId != null) {
            viewModelScope.launch {
                chatRepository.cancelGeneration(clientId, partial)
            }
        }
    }

    /// 同步发送状态
    private fun applySendState(state: ChatSendState) {
        when (state) {
            ChatSendState.Uploading -> _state.update { it.copy(busy = true, streaming = false) }
            ChatSendState.Streaming -> _state.update { it.copy(busy = false, streaming = true) }
            ChatSendState.Idle -> _state.update { it.copy(busy = false, streaming = false) }
        }
    }

    /// 发送成功回调
    private fun onSendSuccess(wasNew: Boolean, cid: String, text: String) {
        if (wasNew) {
            _state.update {
                it.copy(serverConversationId = cid, title = text.ifBlank { "图片对话" }.take(30))
            }
        } else {
            _state.update { it.copy(title = it.title.ifBlank { text.ifBlank { "图片对话" }.take(30) }) }
        }
    }

    // 删除失败用户后 ai 回复
    private suspend fun clearFailedAssistant(userLocalId: Long) {
        val messages = _state.value.messages
        val index = messages.indexOfFirst { it.localId == userLocalId }
        if (index >= 0) {
            messages.drop(index + 1)
                .filter { it.role == ChatRole.ASSISTANT && it.status == ChatMessageStatus.FAILED }
                .forEach { chatRepository.deleteLocalMessage(it.localId) }
        }
    }

    /// 清空会话消息
    fun requestClearConversation() {
        _state.update { it.copy(showClearDialog = true) }
    }

    fun dismissClearDialog() {
        _state.update { it.copy(showClearDialog = false) }
    }

    fun clearConversation() {
        val serverId = _state.value.serverConversationId ?: return
        viewModelScope.launch {
            chatRepository.clearMessagesRemote(serverId).fold(
                onSuccess = {
                    chatRepository.clearLocalMessages(serverId)
                    _state.update { it.copy(showClearDialog = false) }
                },
                onFailure = { e ->
                    _state.update { it.copy(showClearDialog = false, error = e.message ?: "清空失败，请重试") }
                },
            )
        }
    }

    /// 模型选择
    fun selectModel(model: AiModelInfo?) {
        _state.update { it.copy(selectedModel = model?.name, showModelPicker = false) }
    }

    fun toggleModelPicker() {
        _state.update { it.copy(showModelPicker = !it.showModelPicker) }
    }

    ///
    private fun observeMessages(conversationId: String) {
        messagesJob?.cancel()
        messagesJob = viewModelScope.launch {
            chatRepository.observeLocalMessages(conversationId).collect { list ->
                _state.update { it.copy(messages = list) }
            }
        }
    }

    /// 拉取历史，轮询补齐
    private fun syncAndPoll(conversationId: String) {
        viewModelScope.launch {
            repeat(MAX_RESUME_POLLS) {
                val stillStreaming = chatRepository.syncMessages(conversationId).getOrNull() ?: false
                if (!stillStreaming) return@launch
                delay(RESUME_POLL_INTERVAL_MS.milliseconds)
            }
        }
    }

    override fun onCleared() {
        sendJob?.cancel()
        super.onCleared()
    }

    private companion object {
        const val MAX_IMAGES = 4
        const val MAX_MESSAGE_LENGTH = 2000
        const val MAX_RESUME_POLLS = 6
        const val RESUME_POLL_INTERVAL_MS = 1500L
    }
}
