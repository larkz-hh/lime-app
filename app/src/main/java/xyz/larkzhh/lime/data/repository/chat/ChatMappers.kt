package xyz.larkzhh.lime.data.repository.chat

import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import xyz.larkzhh.lime.data.local.chat.ConversationEntity
import xyz.larkzhh.lime.data.local.chat.MessageEntity
import xyz.larkzhh.lime.data.network.model.ChatMessageDto
import xyz.larkzhh.lime.data.network.model.ConversationDto
import xyz.larkzhh.lime.domain.model.ChatConversation
import xyz.larkzhh.lime.domain.model.ChatMessage
import xyz.larkzhh.lime.domain.model.ChatMessageStatus
import xyz.larkzhh.lime.domain.model.ChatRole
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * AI 聊天数据层映射
 */
internal val chatJson = Json { ignoreUnknownKeys = true }

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
internal fun ChatMessageDto.toDomain(conversationId: Long): ChatMessage = ChatMessage(
    conversationId = conversationId,
    serverId = id,
    role = if (role == "user") ChatRole.USER else ChatRole.ASSISTANT,
    content = content,
    images = images.orEmpty(),
    status = ChatMessageStatus.DONE,
    createTime = createTime.toEpochMillis().ifPositive() ?: System.currentTimeMillis(),
)

internal fun MessageEntity.toDomain(): ChatMessage = ChatMessage(
    localId = localId,
    conversationId = conversationId,
    serverId = serverId,
    role = if (role == "user") ChatRole.USER else ChatRole.ASSISTANT,
    content = content,
    images = images.fromJson(),
    localImageUris = localImageUris.fromJson(),
    status = runCatching { ChatMessageStatus.valueOf(status) }.getOrDefault(ChatMessageStatus.DONE),
    createTime = createTime,
    attemptCount = attemptCount,
    nextRetryAt = nextRetryAt,
)

internal fun ChatMessage.toEntity(): MessageEntity = MessageEntity(
    localId = localId,
    conversationId = conversationId,
    serverId = serverId,
    role = if (role == ChatRole.USER) "user" else "assistant",
    content = content,
    images = images.toJson(),
    localImageUris = localImageUris.toJson(),
    status = status.name,
    createTime = createTime,
    attemptCount = attemptCount,
    nextRetryAt = nextRetryAt,
)

internal fun List<String>.toJson(): String =
    chatJson.encodeToString(ListSerializer(String.serializer()), this)

internal fun String?.fromJson(): List<String> =
    this?.let {
        runCatching { chatJson.decodeFromString(ListSerializer(String.serializer()), it) }
            .getOrDefault(emptyList())
    } ?: emptyList()

internal fun String?.toEpochMillis(): Long? = this?.let {
    runCatching {
        LocalDateTime.parse(it).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }.getOrNull()
}

internal fun Long?.ifPositive(): Long? = this?.takeIf { it > 0 }
