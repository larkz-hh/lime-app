package xyz.larkzhh.lime.data.network.model

data class UserData(
    val id: Long,
    val email: String? = null,// 后端脱敏
    val nickname: String,
    val handle: String,
    val uid: String? = null,
    val bio: String?,
    val avatar: String?,
    val backgroundImage: String?,
    val gender: Int?,
    val birthday: String?,
    val region: String?,
    val role: String,
    val likePrivate: Boolean = false,// 点赞列表是否私密
    val favPrivate: Boolean = false,// 收藏列表是否私密
    val followingCount: Int? = null,
    val followerCount: Int? = null,
    val noteCount: Int? = null,
    val totalLikeCount: Int? = null,
    val totalFavCount: Int? = null,
    val isFollowing: Boolean? = null,
    val isFollowedBack: Boolean? = null,
)

/// 修改个人资料请求（bio、region 传 "" 可清空）
data class UpdateProfileRequest(
    val nickname: String? = null,
    val bio: String? = null,
    val gender: Int? = null,
    val birthday: String? = null,
    val region: String? = null,
)

data class FollowListItem(
    val id: Long,
    val nickname: String,
    val handle: String,
    val avatar: String?,
    val bio: String?,
    val isFollowing: Boolean? = null,
    val isFollowedBack: Boolean? = null,
)

data class FollowListResponse(
    val items: List<FollowListItem>,
    val nextCursor: String?,
    val hasMore: Boolean,
)