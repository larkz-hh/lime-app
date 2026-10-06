package xyz.larkzhh.lime.ui.components

import androidx.annotation.StringRes
import xyz.larkzhh.lime.core.ui.R
import xyz.larkzhh.lime.domain.model.FollowActionState

/// 按钮文案资源
@get:StringRes
val FollowActionState.labelRes: Int
    get() = when (this) {
        FollowActionState.Follow -> R.string.follow_action_follow
        FollowActionState.Following -> R.string.follow_action_following
        FollowActionState.Mutual -> R.string.follow_action_mutual
    }
