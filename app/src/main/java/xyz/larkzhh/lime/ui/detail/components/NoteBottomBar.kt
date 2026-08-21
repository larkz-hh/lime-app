package xyz.larkzhh.lime.ui.detail.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import xyz.larkzhh.lime.R
import xyz.larkzhh.lime.data.network.model.NoteDetailData
import xyz.larkzhh.lime.ui.components.LikeButton
import xyz.larkzhh.lime.ui.theme.LimeDark
import xyz.larkzhh.lime.ui.theme.LimeGray
import xyz.larkzhh.lime.ui.theme.LimeLightGray

@Composable
fun NoteBottomBar(
    note: NoteDetailData,
    onToggleLike: () -> Unit,
    onToggleFavorite: () -> Unit,
    onCommentClick: () -> Unit,
    // 颜色/样式参数，默认值即详情页原样；视频页浮层传透明背景+白色内容
    containerColor: Color = MaterialTheme.colorScheme.background,
    contentColor: Color = LimeDark,
    inputBackground: Color = LimeLightGray,
    elevated: Boolean = true,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shadowElevation = if (elevated) 8.dp else 0.dp,
        color = containerColor,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // 评论输入框
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(50))
                    .background(inputBackground)
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                        onClick = onCommentClick,
                    )
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                Text(text = "说点什么…", color = LimeGray, fontSize = 13.sp)
            }

            // 点赞
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                LikeButton(
                    liked = note.liked,
                    onToggle = onToggleLike,
                    iconSize = 24.dp,
                    animationSize = 48.dp,
                    inactiveColor = contentColor,
                )
                Text(
                    text = if (note.likeCount > 0) note.likeCount.toString() else "点赞",
                    fontSize = 12.sp,
                    color = contentColor,
                )
            }

            // 收藏
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() },
                            onClick = onToggleFavorite,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(if (note.favorited) R.drawable.ic_favorite_filled else R.drawable.ic_favorite),
                        contentDescription = if (note.favorited) "取消收藏" else "收藏",
                        tint = if (note.favorited) Color(0xFFFFD700) else contentColor,
                        modifier = Modifier.size(24.dp),
                    )
                }
                Text(
                    text = if (note.favCount > 0) note.favCount.toString() else "收藏",
                    fontSize = 12.sp,
                    color = contentColor,
                )
            }

            // 评论
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                IconButton(onClick = onCommentClick, modifier = Modifier.size(32.dp)) {
                    Icon(
                        painter = painterResource(R.drawable.ic_chat),
                        contentDescription = "评论",
                        tint = contentColor,
                        modifier = Modifier.size(22.dp),
                    )
                }
                Text(
                    text = if (note.commentCount > 0) note.commentCount.toString() else "评论",
                    fontSize = 12.sp,
                    color = contentColor,
                )
            }
        }
    }
}