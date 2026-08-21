package xyz.larkzhh.lime.ui.video.feed.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import xyz.larkzhh.lime.R
import xyz.larkzhh.lime.data.network.model.CommentData
import xyz.larkzhh.lime.data.network.model.ReplyData
import xyz.larkzhh.lime.ui.detail.comment.components.CommentCard
import xyz.larkzhh.lime.ui.detail.comment.components.CommentHeader
import xyz.larkzhh.lime.ui.detail.comment.components.CommentInputBar
import xyz.larkzhh.lime.ui.detail.comment.viewmodel.CommentSort
import xyz.larkzhh.lime.ui.detail.comment.viewmodel.CommentUiState
import xyz.larkzhh.lime.ui.detail.comment.viewmodel.ReplyTarget
import xyz.larkzhh.lime.ui.theme.LimeGray
import xyz.larkzhh.lime.ui.theme.LimeLightGray
import xyz.larkzhh.lime.ui.theme.LimePrimary

/// 视频页评论抽屉
@Composable
fun CommentDrawer(
    visible: Boolean,
    onDismiss: () -> Unit,
    commentUiState: CommentUiState,
    baseCommentCount: Int,// 当前视频原始评论数
    currentUserAvatar: String?,
    onSortChange: (CommentSort) -> Unit,
    onLoadMore: () -> Unit,
    onCommentLike: (Long) -> Unit,
    onReply: (ReplyTarget) -> Unit,
    onLoadMoreReplies: (Long) -> Unit,
    onReplyLike: (commentId: Long, replyId: Long) -> Unit,
    onCommentImageClick: (images: List<String>, index: Int) -> Unit,
    onCommentLongPress: (CommentData) -> Unit,
    onCommentReplyLongPress: (commentId: Long, reply: ReplyData) -> Unit,
    onAuthorClick: (Long) -> Unit,
    onCommentBoxClick: () -> Unit,
    onVoiceClick: () -> Unit,
    onAlbumClick: () -> Unit,
    heightFraction: Float = 0.7f,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // 蒙层
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(animationSpec = tween(200)),
            exit = fadeOut(animationSpec = tween(200)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f))
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                        onClick = onDismiss,
                    ),
            )
        }

        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically(
                animationSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing),
                initialOffsetY = { it },
            ),
            exit = slideOutVertically(
                animationSpec = tween(durationMillis = 300, easing = FastOutLinearInEasing),
                targetOffsetY = { it },
            ),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            val listState = rememberLazyListState()
            // 触底加载更多
            LaunchedEffect(listState.canScrollForward) {
                if (!listState.canScrollForward && commentUiState.hasMore && !commentUiState.isLoadingMore) {
                    onLoadMore()
                }
            }
            // 评论语音播放互斥
            var playingVoiceId by remember { mutableStateOf<Long?>(null) }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(heightFraction)
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                    .background(Color.White)
                    .navigationBarsPadding()
                    // 消费触摸，防止穿透
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                        onClick = {},
                    ),
            ) {
                CommentHeader(
                    commentCount = baseCommentCount + commentUiState.commentCountDelta,
                    sort = commentUiState.sort,
                    onSortChange = onSortChange,
                )

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    state = listState,
                ) {
                    // 无评论空态
                    if (!commentUiState.isLoading && commentUiState.comments.isEmpty()) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Image(
                                    painter = painterResource(R.drawable.img_no_comments),
                                    contentDescription = null,
                                    modifier = Modifier.size(180.dp),
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(text = "这是一片荒草地", fontSize = 13.sp, color = LimeGray)
                            }
                        }
                    }

                    // 评论列表
                    items(commentUiState.comments, key = { it.id }) { comment ->
                        CommentCard(
                            comment = comment,
                            expandedReplies = commentUiState.expandedReplies[comment.id],
                            onLike = { onCommentLike(comment.id) },
                            onReply = onReply,
                            onLoadMoreReplies = { onLoadMoreReplies(comment.id) },
                            onReplyLike = { replyId -> onReplyLike(comment.id, replyId) },
                            onImageClick = onCommentImageClick,
                            playingVoiceId = playingVoiceId,
                            onVoicePlay = { id -> playingVoiceId = id },
                            onVoiceStop = { playingVoiceId = null },
                            onLongPress = { onCommentLongPress(comment) },
                            onReplyLongPress = { reply -> onCommentReplyLongPress(comment.id, reply) },
                            onAuthorClick = onAuthorClick,
                        )
                        HorizontalDivider(color = LimeLightGray, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp))
                    }

                    // 加载更多
                    if (commentUiState.isLoadingMore) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = LimePrimary, strokeWidth = 2.dp)
                            }
                        }
                    }

                    // 到底了
                    if (!commentUiState.hasMore && commentUiState.comments.isNotEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 20.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(text = "- 到底了 -", fontSize = 12.sp, color = LimeGray)
                            }
                        }
                    }
                }

                // 底部输入栏
                CommentInputBar(
                    currentUserAvatar = currentUserAvatar,
                    onCommentClick = onCommentBoxClick,
                    onVoiceClick = onVoiceClick,
                    onAlbumClick = onAlbumClick,
                    showAvatar = false,
                )
            }
        }
    }
}
