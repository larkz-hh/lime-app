package xyz.larkzhh.lime.ui.detail.comment.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import xyz.larkzhh.lime.ui.theme.LimeGray
import xyz.larkzhh.lime.ui.theme.LimeLightGray

/**
 * 评论区输入栏
 *
 * @param currentUserAvatar 当前登录用户头像
 * @param onCommentClick 点击评论框
 * @param onVoiceClick 点击录音入口
 * @param onAlbumClick 点击相册入口
 */
@Composable
fun CommentInputBar(
    currentUserAvatar: String?,
    onCommentClick: () -> Unit,
    onVoiceClick: () -> Unit,
    onAlbumClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // 登录用户头像
        AsyncImage(
            model = currentUserAvatar,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(LimeLightGray),
        )

        // 评论输入框
        Row(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(50))
                .background(LimeLightGray)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = onCommentClick,
                )
                .padding(start = 14.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "说点什么…",
                color = LimeGray,
                fontSize = 13.sp,
                modifier = Modifier.weight(1f),
            )
            // 录音
            IconButton(onClick = onVoiceClick, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Outlined.Mic,
                    contentDescription = "录音",
                    tint = LimeGray,
                    modifier = Modifier.size(20.dp)
                )
            }
            // 相册
            IconButton(onClick = onAlbumClick, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Outlined.Image,
                    contentDescription = "相册",
                    tint = LimeGray,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
