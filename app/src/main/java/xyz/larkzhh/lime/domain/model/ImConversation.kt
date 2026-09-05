package xyz.larkzhh.lime.domain.model

import com.tencent.imsdk.v2.V2TIMConversation
import com.tencent.imsdk.v2.V2TIMMessage

/**
 * 私信会话列表项领域模型
 */
data class ImConversation(
    //c2c_<userID>
    val conversationId: String,
    val showName: String,
    val faceUrl: String? = null,
    val lastMessageText: String = "",
    val unreadCount: Int = 0,
    val timestamp: Long = 0L,
)

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
