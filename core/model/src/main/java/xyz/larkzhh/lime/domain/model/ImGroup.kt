package xyz.larkzhh.lime.domain.model

/// 群聊摘要
data class ImGroup(
    val groupId: String,
    val name: String,
    val introduction: String?,
    val faceUrl: String?,
    val ownerId: String,
    val memberCount: Int,
)
