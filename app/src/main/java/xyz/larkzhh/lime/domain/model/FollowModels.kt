package xyz.larkzhh.lime.domain.model


data class FollowRelation(
    val following: Boolean,
    val followedBack: Boolean,
)

/// 关注按钮状态
enum class FollowActionState {
    Follow,
    Following,
    Mutual,
}

fun FollowRelation?.toFollowActionState(): FollowActionState = when {
    this != null && following && followedBack -> FollowActionState.Mutual
    this != null && following -> FollowActionState.Following
    else -> FollowActionState.Follow
}

/// 按钮文案
val FollowActionState.label: String
    get() = when (this) {
        FollowActionState.Follow -> "关注"
        FollowActionState.Following -> "已关注"
        FollowActionState.Mutual -> "互相关注"
    }
