package xyz.larkzhh.lime.ui.ai

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.tencent.mmkv.MMKV
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import xyz.larkzhh.lime.R
import xyz.larkzhh.lime.data.repository.chat.ChatSendEngine
import xyz.larkzhh.lime.data.repository.chat.ChatSendState
import xyz.larkzhh.lime.domain.model.AiModelInfo
import xyz.larkzhh.lime.domain.model.ChatConversation
import xyz.larkzhh.lime.domain.model.ChatMessage
import xyz.larkzhh.lime.domain.model.ChatMessageStatus
import xyz.larkzhh.lime.domain.model.ChatNote
import xyz.larkzhh.lime.domain.model.ChatRole
import xyz.larkzhh.lime.domain.repository.ChatRepository
import xyz.larkzhh.lime.navigation.PendingChatStore
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
    val selectedNote: ChatNote? = null,
    val requestInputFocus: Boolean = false,
    val webSearch: Boolean = true,
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
    @param:ApplicationContext private val context: Context,
) : ViewModel() {

    private val _state = MutableStateFlow(AiChatUiState())
    val state: StateFlow<AiChatUiState> = _state.asStateFlow()

    private var sendJob: Job? = null
    private var messagesJob: Job? = null
    private val mmkv = MMKV.defaultMMKV()

    /// 历史会话列表
    val conversations: Flow<PagingData<ChatConversation>> =
        chatRepository.conversationsPager().cachedIn(viewModelScope)

    init {
        // 读取联网搜索偏好
        _state.update { it.copy(webSearch = mmkv.decodeBool(KEY_WEB_SEARCH, true)) }
        val enteredId =
            savedStateHandle.get<String>("conversationId") ?: Screen.AiChat.NEW_CONVERSATION

        // 详情页引用笔记与选中文字
        val pendingNote = PendingChatStore.askAiNote
        val pendingText = PendingChatStore.askAiText
        PendingChatStore.askAiNote = null
        PendingChatStore.askAiText = null

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
            if (pendingNote != null || !pendingText.isNullOrEmpty()) {
                _state.update {
                    it.copy(
                        selectedNote = pendingNote,
                        inputText = pendingText.orEmpty(),
                        requestInputFocus = true,
                    )
                }
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
                title = context.getString(R.string.ai_new_chat),
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
                    _state.update { s -> s.copy(error = e.message ?: context.getString(R.string.ai_delete_failed)) }
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
                    _state.update { s -> s.copy(error = e.message ?: context.getString(R.string.ai_clear_failed)) }
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
            _state.update { it.copy(error = context.getString(R.string.ai_offline_cannot_send)) }
            return
        }
        if (s.pendingImages.any { it.state == PendingImageState.FAILED }) {
            _state.update { it.copy(error = context.getString(R.string.ai_image_upload_failed)) }
            return
        }
        val images = s.pendingImages.map { it.localUri }
        val note = s.selectedNote
        val conversationId = s.localConversationId
        val wasNew = s.serverConversationId == null
        val model = s.selectedModel
        val search = s.webSearch

        _state.update { it.copy(inputText = "", pendingImages = emptyList(), selectedNote = null) }

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
                    note = note,
                    status = ChatMessageStatus.SENDING,
                )
            )
            val result = chatSendEngine.send(
                conversationId = conversationId,
                userMessageLocalId = userLocalId,
                messageClientId = messageClientId,
                displayText = text,
                imageLocalUris = images,
                noteId = note?.id,
                search = search,
                model = model,
                onState = ::applySendState,
            )
            result.onSuccess { cid -> onSendSuccess(wasNew, cid, text) }
        }
    }

    /// 选择引用笔记
    fun selectNote(note: ChatNote) {
        _state.update { it.copy(selectedNote = note) }
    }

    /// 移除引用笔记
    fun removeNote() {
        _state.update { it.copy(selectedNote = null) }
    }

    /// 设置联网搜索
    fun setWebSearch(enabled: Boolean) {
        mmkv.encode(KEY_WEB_SEARCH, enabled)
        _state.update { it.copy(webSearch = enabled) }
    }

    /// 输入框聚焦请求消费
    fun onInputFocusConsumed() {
        _state.update { it.copy(requestInputFocus = false) }
    }

    /// 重发失败的用户消息
    fun retryMessage(message: ChatMessage) {
        if (_state.value.busy || _state.value.streaming) return
        if (_state.value.isOffline) {
            _state.update { it.copy(error = context.getString(R.string.ai_offline_cannot_resend)) }
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
                search = _state.value.webSearch,
                model = model,
                onState = ::applySendState,
            )
            result.onSuccess { cid -> onSendSuccess(wasNew, cid, message.content) }
        }
    }

    /// 重新生成某条 AI 回复：删除旧助手回复，复用其前置用户消息重新生成
    fun regenerate(assistant: ChatMessage) {
        if (_state.value.busy || _state.value.streaming) return
        if (_state.value.isOffline) {
            _state.update { it.copy(error = context.getString(R.string.ai_offline_cannot_regenerate)) }
            return
        }
        val msgs = _state.value.messages
        val index = msgs.indexOfFirst { it.localId == assistant.localId }
        if (index <= 0) return
        val userMsg = msgs[index - 1].takeIf { it.role == ChatRole.USER } ?: return
        val conversationId = _state.value.localConversationId
        val wasNew = _state.value.serverConversationId == null
        val model = _state.value.selectedModel

        sendJob?.cancel()
        sendJob = viewModelScope.launch {
            // 删除旧 AI 回复
            if (assistant.serverId != null) {
                chatRepository.deleteMessageRemote(conversationId, assistant.serverId)
            }
            chatRepository.deleteLocalMessage(assistant.localId)
            // 复用前置用户消息
            val result = chatSendEngine.send(
                conversationId = conversationId,
                userMessageLocalId = userMsg.localId,
                messageClientId = userMsg.clientId ?: UUID.randomUUID().toString(),
                displayText = userMsg.content,
                imageLocalUris = userMsg.localImageUris,
                search = _state.value.webSearch,
                model = model,
                onState = ::applySendState,
            )
            result.onSuccess { cid -> onSendSuccess(wasNew, cid, userMsg.content) }
        }
    }

    /// 删除一对消息
    fun deleteMessagePair(message: ChatMessage) {
        val conversationId = _state.value.localConversationId
        val msgs = _state.value.messages
        val index = msgs.indexOfFirst { it.localId == message.localId }
        if (index < 0) return

        val toDelete = mutableListOf<ChatMessage>()
        if (message.role == ChatRole.USER) {
            toDelete += message
            val next = msgs.getOrNull(index + 1)
            if (next != null && next.role == ChatRole.ASSISTANT) toDelete += next
        } else {
            val prev = msgs.getOrNull(index - 1)
            if (prev != null && prev.role == ChatRole.USER) toDelete += prev
            toDelete += message
        }

        viewModelScope.launch {
            toDelete.forEach { m ->
                if (m.serverId != null) {
                    chatRepository.deleteMessageRemote(conversationId, m.serverId)
                }
                chatRepository.deleteLocalMessage(m.localId)
            }
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
        val pending = msgs.filter {
            (it.role == ChatRole.USER && it.status == ChatMessageStatus.SENDING) ||
                (it.role == ChatRole.ASSISTANT && it.status == ChatMessageStatus.STREAMING)
        }
        if (pending.isNotEmpty()) {
            viewModelScope.launch {
                pending.forEach { m ->
                    if (m.role == ChatRole.USER) {
                        chatRepository.updateMessageStatus(m.localId, ChatMessageStatus.DONE)
                    } else {
                        chatRepository.updateMessage(m.localId, null, m.content, ChatMessageStatus.STOPPED)
                    }
                }
            }
        }
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
                it.copy(serverConversationId = cid, title = text.ifBlank { context.getString(R.string.ai_image_chat_title) }.take(30))
            }
        } else {
            _state.update { it.copy(title = it.title.ifBlank { text.ifBlank { context.getString(R.string.ai_image_chat_title) }.take(30) }) }
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
                    _state.update { it.copy(showClearDialog = false, error = e.message ?: context.getString(R.string.ai_clear_failed)) }
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
        const val KEY_WEB_SEARCH = "ai_web_search"
    }
}
