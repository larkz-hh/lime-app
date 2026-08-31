package xyz.larkzhh.lime.data.repository.chat

import androidx.core.net.toUri
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import xyz.larkzhh.lime.domain.model.ChatConversation
import xyz.larkzhh.lime.domain.model.ChatMessage
import xyz.larkzhh.lime.domain.model.ChatMessageStatus
import xyz.larkzhh.lime.domain.model.ChatRole
import xyz.larkzhh.lime.domain.model.ChatStreamEvent
import xyz.larkzhh.lime.domain.repository.ChatRepository
import xyz.larkzhh.lime.domain.repository.NoteRepository
import javax.inject.Inject
import javax.inject.Singleton

/// 发送进度状态
sealed interface ChatSendState {
    data object Uploading : ChatSendState
    data object Streaming : ChatSendState
    data object Idle : ChatSendState
}

/**
 * AI 聊天发送引擎
 */
@Singleton
class ChatSendEngine @Inject constructor(
    private val chatRepository: ChatRepository,
    private val noteRepository: NoteRepository,
) {

    suspend fun send(
        conversationId: String,
        userMessageLocalId: Long,
        messageClientId: String,
        displayText: String,
        imageLocalUris: List<String>,
        noteId: Long? = null,
        search: Boolean? = null,
        model: String?,
        onState: suspend (ChatSendState) -> Unit = {},
    ): Result<String> {
        val serverText = displayText.ifBlank { "请帮我看看这张图片" }// 无文字
        // 标记发送中
        chatRepository.updateMessageStatus(userMessageLocalId, ChatMessageStatus.SENDING)

        // 上传图片
        val uploadedUrls = mutableListOf<String>()
        try {
            onState(ChatSendState.Uploading)
            for (uri in imageLocalUris) {
                val url = noteRepository.uploadImage(uri.toUri()).getOrNull()
                if (url == null) {
                    chatRepository.updateMessageStatus(userMessageLocalId, ChatMessageStatus.FAILED)
                    onState(ChatSendState.Idle)
                    return Result.failure(IllegalStateException("图片上传失败，请重试"))
                }
                uploadedUrls += url
            }
        } catch (ce: CancellationException) {
            // 上传阶段取消
            withContext(NonCancellable) {
                chatRepository.updateMessageStatus(userMessageLocalId, ChatMessageStatus.FAILED)
                onState(ChatSendState.Idle)
            }
            throw ce
        }

        onState(ChatSendState.Streaming)
        val assistantLocalId = chatRepository.saveMessage(
            ChatMessage(
                conversationId = conversationId,
                role = ChatRole.ASSISTANT,
                status = ChatMessageStatus.STREAMING,
            )
        )
        val buffer = StringBuilder()
        var lastPersist = System.currentTimeMillis()
        var done: ChatStreamEvent.Done? = null
        var streamError: String? = null

        try {
            chatRepository.chatStream(
                conversationId = conversationId,
                messageClientId = messageClientId,
                message = serverText.take(MAX_MESSAGE_LENGTH),
                imageUrls = uploadedUrls.ifEmpty { null },
                noteId = noteId,
                model = model,
                search = search,
            ).collect { event ->
                when (event) {
                    is ChatStreamEvent.Delta -> {
                        buffer.append(event.content)
                        val now = System.currentTimeMillis()
                        if (now - lastPersist > PERSIST_INTERVAL_MS) {
                            lastPersist = now
                            chatRepository.updateMessage(
                                assistantLocalId, null, buffer.toString(), ChatMessageStatus.STREAMING
                            )
                        }
                    }

                    is ChatStreamEvent.Done -> done = event
                    is ChatStreamEvent.Error -> streamError = event.message
                }
            }
        } catch (ce: CancellationException) {
            // 流式阶段取消
            withContext(NonCancellable) {
                chatRepository.updateMessage(
                    assistantLocalId, null, buffer.toString(), ChatMessageStatus.STOPPED
                )
                chatRepository.updateMessageStatus(userMessageLocalId, ChatMessageStatus.DONE)
            }
            throw ce
        } catch (e: Exception) {
            withContext(NonCancellable) {
                chatRepository.updateMessage(
                    assistantLocalId, null, buffer.toString(), ChatMessageStatus.FAILED
                )
                chatRepository.updateMessageStatus(userMessageLocalId, ChatMessageStatus.FAILED)
            }
            return Result.failure(e)
        } finally {
            onState(ChatSendState.Idle)
        }

        val doneEvent = done
        return if (doneEvent != null) {
            chatRepository.updateMessageWithImages(
                userMessageLocalId, doneEvent.userMessageId, displayText,
                uploadedUrls.ifEmpty { null }, ChatMessageStatus.DONE,
            )
            chatRepository.updateMessage(
                assistantLocalId, doneEvent.assistantMessageId, buffer.toString(),
                ChatMessageStatus.DONE,
            )
            val existing = chatRepository.getLocalConversation(conversationId)
            chatRepository.saveConversation(
                ChatConversation(
                    id = conversationId,
                    title = existing?.title ?: displayText.ifBlank { "图片对话" }.take(30),
                    updateTime = System.currentTimeMillis(),
                )
            )
            Result.success(conversationId)
        } else {
            chatRepository.updateMessage(
                assistantLocalId, null, buffer.toString(), ChatMessageStatus.FAILED
            )
            chatRepository.updateMessageStatus(userMessageLocalId, ChatMessageStatus.FAILED)
            Result.failure(IllegalStateException(streamError ?: "生成失败，请重试"))
        }
    }

    private companion object {
        const val MAX_MESSAGE_LENGTH = 2000
        const val PERSIST_INTERVAL_MS = 120L
    }
}
