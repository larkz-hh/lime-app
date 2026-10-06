package xyz.larkzhh.lime.data.im

import com.tencent.imsdk.v2.V2TIMConversation
import com.tencent.imsdk.v2.V2TIMGroupInfo
import com.tencent.imsdk.v2.V2TIMImageElem
import com.tencent.imsdk.v2.V2TIMMessage
import xyz.larkzhh.lime.domain.model.ImConversation
import xyz.larkzhh.lime.domain.model.ImGroup
import xyz.larkzhh.lime.domain.model.ImMessage

fun V2TIMMessage.toImMessage(selfUserId: String?): ImMessage? {
    // 被撤回的消息
    if (status == V2TIMMessage.V2TIM_MSG_STATUS_LOCAL_REVOKED) {
        return ImMessage(
            id = msgID ?: "",
            senderId = sender ?: "",
            isSelf = sender == selfUserId,
            timestamp = timestamp,
            isRevoked = true,
            groupId = groupID,
        )
    }
    return when (elemType) {
        V2TIMMessage.V2TIM_ELEM_TYPE_TEXT -> ImMessage(
            id = msgID ?: "",
            senderId = sender ?: "",
            isSelf = sender == selfUserId,
            timestamp = timestamp,
            text = textElem?.text ?: "",
            groupId = groupID,
        )

        V2TIMMessage.V2TIM_ELEM_TYPE_IMAGE -> {
            // 发送方本地优先，接收方历史消息 url
            val originUrl = imageElem?.imageList
                ?.firstOrNull { it.type == V2TIMImageElem.V2TIM_IMAGE_TYPE_ORIGIN }
                ?.url
            ImMessage(
                id = msgID ?: "",
                senderId = sender ?: "",
                isSelf = sender == selfUserId,
                timestamp = timestamp,
                imagePath = imageElem?.path?.takeIf { it.isNotBlank() },
                imageUrl = originUrl,
                groupId = groupID,
            )
        }

        else -> null
    }
}

fun V2TIMConversation.toImConversation(): ImConversation {
    val last = lastMessage
    val lastText = when (last?.elemType) {
        V2TIMMessage.V2TIM_ELEM_TYPE_TEXT -> last.textElem?.text ?: ""
        V2TIMMessage.V2TIM_ELEM_TYPE_IMAGE -> "[图片]"
        else -> ""
    }
    return ImConversation(
        conversationId = conversationID ?: "",
        showName = showName ?: "",
        faceUrl = faceUrl,
        lastMessageText = lastText,
        unreadCount = unreadCount,
        timestamp = last?.timestamp ?: 0L,
    )
}

fun V2TIMGroupInfo.toImGroup(): ImGroup = ImGroup(
    groupId = groupID ?: "",
    name = groupName ?: "",
    introduction = introduction,
    faceUrl = faceUrl,
    ownerId = owner ?: "",
    memberCount = memberCount,
)
