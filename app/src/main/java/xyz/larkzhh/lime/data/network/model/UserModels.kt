package xyz.larkzhh.lime.data.network.model

data class UserData(
    val id: Long,
    val email: String,
    val nickname: String,
    val handle: String,
    val bio: String?,
    val avatar: String?,
    val backgroundImage: String?,
    val gender: Int?,
    val birthday: String?,
    val region: String?,
    val role: String,
    val likePrivate: Boolean = false,// 点赞列表是否私密
    val favPrivate: Boolean = false,// 收藏列表是否私密
)

/// 修改个人资料请求（null 字段不序列化，bio/region 传 "" 可清空）
data class UpdateProfileRequest(
    val nickname: String? = null,
    val bio: String? = null,
    val gender: Int? = null,
    val birthday: String? = null,
    val region: String? = null,
)