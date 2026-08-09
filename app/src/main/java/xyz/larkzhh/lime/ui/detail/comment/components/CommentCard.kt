package xyz.larkzhh.lime.ui.detail.comment.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import xyz.larkzhh.lime.ui.detail.comment.viewmodel.ExpandedRepliesState
import xyz.larkzhh.lime.ui.detail.comment.viewmodel.ReplyTarget
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
    onImageClick: (images: List<String>, index: Int) -> Unit = { _, _ -> },
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

            // 评论文字内容
            if (!comment.content.isNullOrBlank()) {
                Text(
                    text = comment.content,
                    fontSize = 15.sp,
                    color = LimeDark,
                    lineHeight = 22.sp,
                )
            }

            // 评论图片
            val images = comment.images
            if (!images.isNullOrEmpty()) {
                if (!comment.content.isNullOrBlank()) Spacer(modifier = Modifier.height(6.dp))
                CommentImageGrid(
                    images = images,
                    onImageClick = { index -> onImageClick(images, index) },
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // 时间、ip、回复、点赞
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = formatRelativeTime(comment.createTime),
                    fontSize = 12.sp,
                    color = LimeGray,
                )
                if (!comment.ipLocation.isNullOrBlank()) {
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(text = comment.ipLocation, fontSize = 12.sp, color = LimeGray)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "回复",
                    fontSize = 12.sp,
                    color = LimeGray,
                    modifier = Modifier.clickable {
                        onReply(ReplyTarget(comment.id, null, comment.author.nickname))
                    },
                )
                Spacer(modifier = Modifier.weight(1f))
                LikeButton(
                    liked = comment.liked,
                    onToggle = onLike,
                    iconSize = 16.dp,
                    animationSize = 32.dp,
                )
                if (comment.likeCount > 0) {
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = comment.likeCount.toString(),
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
                            onImageClick = onImageClick,
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
    onImageClick: (images: List<String>, index: Int) -> Unit = { _, _ -> },
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

            // 回复文字内容
            val hasText = !reply.content.isNullOrBlank()
            if (reply.replyToNickname != null || hasText) {
                val contentText = buildAnnotatedString {
                    if (reply.replyToNickname != null) {
                        append("回复 ")
                        withStyle(SpanStyle(color = LimePrimary)) { append(reply.replyToNickname) }
                        if (hasText) append(" ")
                    }
                    if (hasText) append(reply.content!!)
                }
                Text(text = contentText, fontSize = 14.sp, color = LimeDark, lineHeight = 20.sp)
            }

            // 回复图片
            val images = reply.images
            if (!images.isNullOrEmpty()) {
                if (reply.replyToNickname != null || hasText) Spacer(modifier = Modifier.height(4.dp))
                CommentImageGrid(
                    images = images,
                    onImageClick = { index -> onImageClick(images, index) },
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // 时间、ip、回复、点赞
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = formatRelativeTime(reply.createTime), fontSize = 11.sp, color = LimeGray)
                if (!reply.ipLocation.isNullOrBlank()) {
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(text = reply.ipLocation, fontSize = 11.sp, color = LimeGray)
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "回复",
                    fontSize = 11.sp,
                    color = LimeGray,
                    modifier = Modifier.clickable { onReply() },
                )
                Spacer(modifier = Modifier.weight(1f))
                LikeButton(liked = reply.liked, onToggle = onLike, iconSize = 14.dp, animationSize = 28.dp)
                if (reply.likeCount > 0) {
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = reply.likeCount.toString(),
                        fontSize = 11.sp,
                        color = LimeGray,
                    )
                }
            }
        }
    }
}

/// 评论/回复图片
@Composable
private fun CommentImageGrid(
    images: List<String>,
    onImageClick: (index: Int) -> Unit,
) {
    val displayImages = images.take(3)
    val total = images.size

    when (displayImages.size) {
        // 单图高度自适应
        1 -> {
            AsyncImage(
                model = displayImages[0],
                contentDescription = null,
                contentScale = ContentScale.FillWidth,
                modifier = Modifier
                    .fillMaxWidth(0.5f)
                    .heightIn(max = 180.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onImageClick(0) },
            )
        }
        // 两列等宽正方形
        2 -> {
            Row(
                modifier = Modifier.fillMaxWidth(0.55f),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                displayImages.forEachIndexed { index, url ->
                    AsyncImage(
                        model = url,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { onImageClick(index) },
                    )
                }
            }
        }
        // 三列等宽正方形
        else -> {
            Row(
                modifier = Modifier.fillMaxWidth(0.85f),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                displayImages.forEachIndexed { index, url ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { onImageClick(index) },
                    ) {
                        AsyncImage(
                            model = url,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                        if (total > 3 && index == displayImages.lastIndex) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.4f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = "共${total}张",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                )
                            }
                        }
                    }
                }
            }
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
