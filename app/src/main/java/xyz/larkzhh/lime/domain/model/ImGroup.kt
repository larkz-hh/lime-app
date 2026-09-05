package xyz.larkzhh.lime.domain.model

import com.tencent.imsdk.v2.V2TIMGroupInfo

/// 群聊摘要
data class ImGroup(
    val groupId: String,
    val name: String,
    val introduction: String?,
    val faceUrl: String?,
    val ownerId: String,
    val memberCount: Int,
)

fun V2TIMGroupInfo.toImGroup(): ImGroup = ImGroup(
    groupId = groupID ?: "",
    name = groupName ?: "",
    introduction = introduction,
    faceUrl = faceUrl,
    ownerId = owner ?: "",
    memberCount = memberCount,
)
