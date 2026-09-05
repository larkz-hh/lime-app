package xyz.larkzhh.lime.domain.model

import com.tencent.imsdk.v2.V2TIMImageElem
import com.tencent.imsdk.v2.V2TIMMessage

/**
 * 聊天消息领域模型
 */
data class ImMessage(
    val id: String,
    val senderId: String,
    val isSelf: Boolean,
    val timestamp: Long,
    val text: String? = null,
    val imagePath: String? = null,
    val imageUrl: String? = null,
    val isRevoked: Boolean = false,
    val groupId: String? = null,
) {
    val isImage: Boolean get() = text == null
}

data class ImUserProfile(
    val nickname: String?,
    val faceUrl: String?,
)

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
