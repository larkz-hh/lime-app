package xyz.larkzhh.lime.ui.video.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import xyz.larkzhh.lime.R
import xyz.larkzhh.lime.ui.components.FavoriteButton
import xyz.larkzhh.lime.ui.components.LikeButton

// 视频侧边栏
@Composable
fun VideoSideActionBar(
    liked: Boolean,
    likeCount: Int,
    favorited: Boolean,
    favCount: Int,
    commentCount: Int,
    onToggleLike: () -> Unit,
    onToggleFavorite: () -> Unit,
    onCommentClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconSize: Dp = 34.dp,
    animationSize: Dp = 44.dp,
    spacing: Dp = 16.dp,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(spacing),
    ) {
        SideActionItem(
            onClick = onToggleLike,
            count = likeCount,
            label = "点赞",
        ) {
            LikeButton(
                liked = liked,
                onToggle = onToggleLike,
                iconSize = iconSize,
                animationSize = animationSize,
                inactiveColor = Color.White,
            )
        }
        SideActionItem(
            onClick = onCommentClick,
            count = commentCount,
            label = "评论",
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_chat),
                contentDescription = "评论",
                tint = Color.White,
                modifier = Modifier.size(iconSize),
            )
        }
        SideActionItem(
            onClick = onToggleFavorite,
            count = favCount,
            label = "收藏",
        ) {
            FavoriteButton(
                favorited = favorited,
                onToggle = onToggleFavorite,
                modifier = Modifier.size(animationSize),
                iconSize = iconSize,
                inactiveColor = Color.White,
            )
        }
    }
}

@Composable
private fun SideActionItem(
    onClick: () -> Unit,
    count: Int,
    label: String,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier.clickable(
            indication = null,
            interactionSource = remember { MutableInteractionSource() },
            onClick = onClick,
        ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        content()
        Text(
            text = if (count > 0) count.toString() else label,
            color = Color.White,
            fontSize = 12.sp,
            maxLines = 1,
        )
    }
}
