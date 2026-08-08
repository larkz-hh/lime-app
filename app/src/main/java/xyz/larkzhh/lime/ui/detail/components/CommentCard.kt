package xyz.larkzhh.lime.ui.detail.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import xyz.larkzhh.lime.data.network.model.CommentData
import xyz.larkzhh.lime.data.network.model.ReplyData
import xyz.larkzhh.lime.ui.components.LikeButton
import xyz.larkzhh.lime.ui.detail.ExpandedRepliesState
import xyz.larkzhh.lime.ui.detail.ReplyTarget
import xyz.larkzhh.lime.ui.theme.LimeDark
import xyz.larkzhh.lime.ui.theme.LimeGray
import xyz.larkzhh.lime.ui.theme.LimePrimary
import xyz.larkzhh.lime.util.formatRelativeTime

@Composable
fun CommentCard(
    comment: CommentData,
    expandedReplies: ExpandedRepliesState?,
    onLike: () -> Unit,
    onReply: (ReplyTarget) -> Unit,
    onLoadMoreReplies: () -> Unit,
    modifier: Modifier = Modifier,
    onReplyLike: (replyId: Long) -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        // 头像
        AsyncImage(
            model = comment.author.avatar,
            contentDescription = null,
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop,
        )

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    // 昵称、作者标签
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = comment.author.nickname,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = LimeGray,
                        )
                        if (comment.isNoteAuthor) {
                            Spacer(modifier = Modifier.width(4.dp))
                            AuthorBadge()
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // 评论内容
                    Text(
                        text = comment.content,
                        fontSize = 15.sp,
                        color = LimeDark,
                        lineHeight = 22.sp,
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // 时间、ip、回复
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            text = formatRelativeTime(comment.createTime),
                            fontSize = 12.sp,
                            color = LimeGray,
                        )
                        if (!comment.ipLocation.isNullOrBlank()) {
                            Text(text = comment.ipLocation, fontSize = 12.sp, color = LimeGray)
                        }
                        Text(
                            text = "回复",
                            fontSize = 12.sp,
                            color = LimeGray,
                            modifier = Modifier.clickable {
                                onReply(ReplyTarget(comment.id, null, comment.author.nickname))
                            },
                        )
                    }
                }

                // 点赞
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    LikeButton(
                        liked = comment.liked,
                        onToggle = onLike,
                        iconSize = 20.dp,
                        animationSize = 40.dp,
                    )
                    Text(
                        text = if (comment.likeCount > 0) comment.likeCount.toString() else "",
                        fontSize = 11.sp,
                        color = LimeGray,
                    )
                }
            }

            // 回复区域
            val replies = expandedReplies?.replies ?: comment.topReplies
            if (!replies.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    replies.forEach { reply ->
                        ReplyItem(
                            reply = reply,
                            onReply = {
                                onReply(ReplyTarget(comment.id, reply.author.id, reply.author.nickname))
                            },
                            onLike = { onReplyLike(reply.id) },
                        )
                    }
                }

                val isExpanded = expandedReplies != null
                val replyCount = comment.replyCount
                when {
                    expandedReplies?.isLoading == true -> {
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = LimePrimary, strokeWidth = 2.dp)
                        }
                    }
                    expandedReplies?.hasMore == true -> {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "展开更多回复",
                            fontSize = 12.sp,
                            color = LimePrimary,
                            modifier = Modifier.clickable { onLoadMoreReplies() },
                        )
                    }
                    !isExpanded && replyCount > 1 -> {
                        val label = if (replyCount <= 5) "展开${replyCount - 1}条回复" else "展开5条回复"
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = label,
                            fontSize = 12.sp,
                            color = LimePrimary,
                            modifier = Modifier.clickable { onLoadMoreReplies() },
                        )
                    }
                }
            } else if (comment.replyCount > 0 && expandedReplies == null) {
                Spacer(modifier = Modifier.height(6.dp))
                val label = if (comment.replyCount <= 5) "展开${comment.replyCount}条回复" else "展开5条回复"
                Text(
                    text = label,
                    fontSize = 12.sp,
                    color = LimePrimary,
                    modifier = Modifier.clickable { onLoadMoreReplies() },
                )
            }
        }
    }
}

@Composable
private fun ReplyItem(
    reply: ReplyData,
    onReply: () -> Unit,
    onLike: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
    ) {
        AsyncImage(
            model = reply.author.avatar,
            contentDescription = null,
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop,
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = reply.author.nickname,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = LimeGray,
                )
                if (reply.isNoteAuthor) {
                    Spacer(modifier = Modifier.width(4.dp))
                    AuthorBadge()
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            val contentText = if (reply.replyToNickname != null) {
                buildAnnotatedString {
                    append("回复 ")
                    withStyle(SpanStyle(color = LimePrimary)) { append(reply.replyToNickname) }
                    append(" ${reply.content}")
                }
            } else {
                buildAnnotatedString { append(reply.content) }
            }
            Text(text = contentText, fontSize = 14.sp, color = LimeDark, lineHeight = 20.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(text = formatRelativeTime(reply.createTime), fontSize = 11.sp, color = LimeGray)
                if (!reply.ipLocation.isNullOrBlank()) {
                    Text(text = reply.ipLocation, fontSize = 11.sp, color = LimeGray)
                }
                Text(
                    text = "回复",
                    fontSize = 11.sp,
                    color = LimeGray,
                    modifier = Modifier.clickable { onReply() },
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            LikeButton(liked = reply.liked, onToggle = onLike, iconSize = 18.dp, animationSize = 36.dp)
            Text(
                text = if (reply.likeCount > 0) reply.likeCount.toString() else "",
                fontSize = 11.sp,
                color = LimeGray,
            )
        }
    }
}

@Composable
private fun AuthorBadge() {
    Text(
        text = "作者",
        fontSize = 10.sp,
        color = LimePrimary,
        modifier = Modifier
            .padding(horizontal = 4.dp, vertical = 1.dp),
    )
}
