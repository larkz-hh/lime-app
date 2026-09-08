package xyz.larkzhh.lime.domain.model

import androidx.annotation.StringRes
import xyz.larkzhh.lime.R

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

/// 按钮文案资源
@get:StringRes
val FollowActionState.labelRes: Int
    get() = when (this) {
        FollowActionState.Follow -> R.string.follow_action_follow
        FollowActionState.Following -> R.string.follow_action_following
        FollowActionState.Mutual -> R.string.follow_action_mutual
    }
