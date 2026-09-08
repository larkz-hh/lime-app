package xyz.larkzhh.lime.data.mapper

import xyz.larkzhh.lime.data.local.notification.NotificationEntity
import xyz.larkzhh.lime.data.network.model.NotificationData

/**
 * 站内通知数据层映射
 */

internal fun NotificationData.toEntity(): NotificationEntity = NotificationEntity(
    id = id,
    type = type,
    noteId = noteId,
    commentId = commentId,
    content = content,
    parentCommentId = parentCommentId,
    replyToContent = replyToContent,
    noteCover = noteCover,
    isRead = isRead,
    createTime = createTime.orEmpty(),
    senderId = senderId,
    senderNickname = senderNickname,
    senderAvatar = senderAvatar,
)

internal fun NotificationEntity.toData(): NotificationData = NotificationData(
    id = id,
    type = type,
    noteId = noteId,
    commentId = commentId,
    content = content,
    parentCommentId = parentCommentId,
    replyToContent = replyToContent,
    noteCover = noteCover,
    isRead = isRead,
    createTime = createTime,
    senderId = senderId,
    senderNickname = senderNickname,
    senderAvatar = senderAvatar,
)
