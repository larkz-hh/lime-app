package xyz.larkzhh.lime.data.mapper

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import xyz.larkzhh.lime.data.local.ai.ConversationEntity
import xyz.larkzhh.lime.data.local.ai.MessageEntity
import xyz.larkzhh.lime.data.network.model.ChatMessageDto
import xyz.larkzhh.lime.data.network.model.ConversationDto
import xyz.larkzhh.lime.domain.model.ChatConversation
import xyz.larkzhh.lime.domain.model.ChatMessage
import xyz.larkzhh.lime.domain.model.ChatMessageStatus
import xyz.larkzhh.lime.domain.model.ChatNote
import xyz.larkzhh.lime.domain.model.ChatRole
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * AI 聊天数据层映射
 */
private val chatGson = Gson()

/// 会话

internal fun ConversationDto.toDomain(): ChatConversation = ChatConversation(
    id = id,
    title = title,
    updateTime = updateTime.toEpochMillis().ifPositive()
        ?: createTime.toEpochMillis().ifPositive()
        ?: System.currentTimeMillis(),
)

internal fun ConversationEntity.toDomain(): ChatConversation = ChatConversation(
    id = id,
    title = title,
    updateTime = updateTime,
)

internal fun ChatConversation.toEntity(): ConversationEntity = ConversationEntity(
    id = id,
    title = title,
    updateTime = updateTime,
)

/// 消息
internal fun ChatMessageDto.toDomain(conversationId: String): ChatMessage = ChatMessage(
    conversationId = conversationId,
    serverId = id,
    clientId = clientId,
    role = if (role == "user") ChatRole.USER else ChatRole.ASSISTANT,
    content = content,
    images = images.orEmpty(),
    note = noteId?.let { ChatNote(it, noteTitle, noteCover) },
    status = when (status) {
        "streaming" -> ChatMessageStatus.STREAMING
        "failed" -> ChatMessageStatus.FAILED
        "stopped" -> ChatMessageStatus.STOPPED
        else -> ChatMessageStatus.DONE
    },
    createTime = createTime.toEpochMillis().ifPositive() ?: System.currentTimeMillis(),
)

internal fun MessageEntity.toDomain(): ChatMessage = ChatMessage(
    localId = localId,
    conversationId = conversationId,
    serverId = serverId,
    clientId = clientId,
    role = if (role == "user") ChatRole.USER else ChatRole.ASSISTANT,
    content = content,
    images = images.fromJson(),
    localImageUris = localImageUris.fromJson(),
    note = noteId?.let { ChatNote(it, noteTitle, noteCover) },
    status = runCatching { ChatMessageStatus.valueOf(status) }.getOrDefault(ChatMessageStatus.DONE),
    createTime = createTime,
)

internal fun ChatMessage.toEntity(): MessageEntity = MessageEntity(
    localId = localId,
    conversationId = conversationId,
    serverId = serverId,
    clientId = clientId,
    role = if (role == ChatRole.USER) "user" else "assistant",
    content = content,
    images = images.toJson(),
    localImageUris = localImageUris.toJson(),
    noteId = note?.id,
    noteTitle = note?.title,
    noteCover = note?.cover,
    status = status.name,
    createTime = createTime,
)

internal fun List<String>.toJson(): String =
    chatGson.toJson(this)

internal fun String?.fromJson(): List<String> =
    this?.let {
        runCatching {
            chatGson.fromJson<List<String>>(it, object : TypeToken<List<String>>() {}.type)
        }.getOrDefault(emptyList())
    } ?: emptyList()

internal fun String?.toEpochMillis(): Long? = this?.let {
    runCatching {
        LocalDateTime.parse(it).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }.getOrNull()
}

internal fun Long?.ifPositive(): Long? = this?.takeIf { it > 0 }
