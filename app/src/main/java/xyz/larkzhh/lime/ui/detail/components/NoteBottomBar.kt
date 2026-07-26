package xyz.larkzhh.lime.ui.detail.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import xyz.larkzhh.lime.data.network.model.NoteDetailData
import xyz.larkzhh.lime.ui.components.LikeButton
import xyz.larkzhh.lime.ui.theme.LimeGray
import xyz.larkzhh.lime.ui.theme.LimeLightGray
import xyz.larkzhh.lime.ui.theme.LimePrimary

/// 底部栏
@Composable
fun NoteBottomBar(
    note: NoteDetailData,
    onToggleLike: () -> Unit = {},
    onToggleFavorite: () -> Unit = {},
) {
    Surface(
        shadowElevation = 8.dp,
        color = MaterialTheme.colorScheme.background,
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
                    .background(LimeLightGray)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                Text(
                    text = "说点什么…",
                    color = LimeGray,
                    fontSize = 13.sp,
                )
            }

            // 点赞
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                LikeButton(
                    liked = note.liked,
                    onToggle = {},
                    iconSize = 22.dp,
                    animationSize = 32.dp,
                )
                Text(
                    text = note.likeCount.toString(),
                    fontSize = 12.sp,
                    color = LimeGray,
                )
            }

            // 收藏
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                IconButton(
                    onClick = {},
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(
                        imageVector = if (note.favorited) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                        contentDescription = if (note.favorited) "取消收藏" else "收藏",
                        tint = if (note.favorited) LimePrimary else LimeGray,
                        modifier = Modifier.size(22.dp),
                    )
                }
                Text(
                    text = note.favCount.toString(),
                    fontSize = 12.sp,
                    color = LimeGray,
                )
            }

            // 评论
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                IconButton(
                    onClick = {},
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ChatBubbleOutline,
                        contentDescription = "评论",
                        tint = LimeGray,
                        modifier = Modifier.size(22.dp),
                    )
                }
                Text(
                    text = "0",
                    fontSize = 12.sp,
                    color = LimeGray,
                )
            }
        }
    }
}