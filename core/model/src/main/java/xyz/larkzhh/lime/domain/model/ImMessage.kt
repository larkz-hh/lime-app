package xyz.larkzhh.lime.domain.model

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
