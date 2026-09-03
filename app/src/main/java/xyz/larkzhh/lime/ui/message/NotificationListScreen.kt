package xyz.larkzhh.lime.ui.message

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import coil3.compose.AsyncImage
import xyz.larkzhh.lime.data.network.model.NotificationData
import xyz.larkzhh.lime.domain.model.FollowActionState
import xyz.larkzhh.lime.domain.model.FollowRelation
import xyz.larkzhh.lime.domain.model.NotificationType
import xyz.larkzhh.lime.domain.model.toFollowActionState
import xyz.larkzhh.lime.navigation.Screen
import xyz.larkzhh.lime.navigation.navigateToUserProfile
import xyz.larkzhh.lime.ui.components.FollowButton
import xyz.larkzhh.lime.ui.components.UnfollowConfirmDialog
import xyz.larkzhh.lime.ui.theme.LimeGray
import xyz.larkzhh.lime.ui.theme.LimeLightGray
import xyz.larkzhh.lime.ui.theme.LimePrimary
import xyz.larkzhh.lime.ui.theme.LimePrimaryPale
import xyz.larkzhh.lime.ui.theme.LimeTheme
import xyz.larkzhh.lime.ui.theme.LimeWhite
import xyz.larkzhh.lime.util.formatRelativeTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationListScreen(
    navController: NavHostController,
    viewModel: NotificationListViewModel = hiltViewModel(),
) {
    val pageState by viewModel.pageState.collectAsState()
    val items by viewModel.items.collectAsState()
    val relations by viewModel.relations.collectAsState()

    var pendingUnfollow by remember { mutableStateOf<NotificationData?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .statusBarsPadding(),
    ) {
        // 顶部栏（返回键 + 居中标题）
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
        ) {
            IconButton(
                onClick = { navController.popBackStack() },
                modifier = Modifier.align(Alignment.CenterStart),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "返回",
                    tint = MaterialTheme.colorScheme.onBackground,
                )
            }
            Text(
                text = viewModel.category.title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.align(Alignment.Center),
            )
        }
        HorizontalDivider(thickness = 0.5.dp, color = LimeLightGray)

        when {
            pageState.isInitialLoading && items.isEmpty() -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(28.dp),
                        color = LimePrimary,
                        trackColor = LimeWhite,
                        strokeWidth = 2.dp,
                    )
                }
            }

            pageState.error != null && items.isEmpty() -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = pageState.error ?: "加载失败",
                        color = LimeGray,
                        modifier = Modifier.clickable { viewModel.loadInitial() },
                    )
                }
            }

            items.isEmpty() -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = "暂时没有新消息~", color = LimeGray)
                }
            }

            else -> {
                val listState = rememberLazyListState()
                val shouldLoadMore by remember {
                    derivedStateOf {
                        val info = listState.layoutInfo
                        val last = info.visibleItemsInfo.lastOrNull()?.index ?: -1
                        info.totalItemsCount > 0 && last >= info.totalItemsCount - 2
                    }
                }
                LaunchedEffect(shouldLoadMore) {
                    if (shouldLoadMore && pageState.hasMore) viewModel.loadMore()
                }
                LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                    items(items, key = { it.id }) { item ->
                        val dismissState = rememberSwipeToDismissBoxState()
                        SwipeToDismissBox(
                            state = dismissState,
                            enableDismissFromStartToEnd = false,
                            enableDismissFromEndToStart = true,
                            backgroundContent = {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color(0xFFFE2C55))
                                        .padding(end = 24.dp),
                                    contentAlignment = Alignment.CenterEnd,
                                ) {
                                    Text(
                                        text = "删除",
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                    )
                                }
                            },
                            onDismiss = { direction ->
                                if (direction == SwipeToDismissBoxValue.EndToStart) {
                                    viewModel.delete(item)
                                }
                            },
                        ) {
                            NotificationCard(
                                item = item,
                                relation = relations[item.senderId],
                                onCardClick = {
                                    viewModel.markRead(item)
                                    val type = NotificationType.fromCode(item.type)
                                    when (type) {
                                        NotificationType.Follow -> item.senderId?.let {
                                            navController.navigateToUserProfile(it)
                                        }
                                        else -> item.noteId?.let {
                                            navController.navigate(Screen.Detail.createRoute(it.toString()))
                                        }
                                    }
                                },
                                onAvatarClick = {
                                    item.senderId?.let { navController.navigateToUserProfile(it) }
                                },
                                onFollowClick = { item.senderId?.let { viewModel.follow(it) } },
                                onUnfollowClick = { pendingUnfollow = item },
                            )
                        }
                        HorizontalDivider(
                            thickness = 0.5.dp,
                            color = LimeLightGray,
                            modifier = Modifier.padding(start = 72.dp),
                        )
                    }
                    if (pageState.isLoadingMore) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = LimePrimary,
                                    trackColor = LimeWhite,
                                    strokeWidth = 2.dp,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    pendingUnfollow?.let { item ->
        UnfollowConfirmDialog(
            onCancel = { pendingUnfollow = null },
            onConfirm = {
                item.senderId?.let { viewModel.unfollow(it) }
                pendingUnfollow = null
            },
        )
    }
}

@Composable
private fun NotificationCard(
    item: NotificationData,
    relation: FollowRelation?,
    onCardClick: () -> Unit,
    onAvatarClick: () -> Unit,
    onFollowClick: () -> Unit,
    onUnfollowClick: () -> Unit,
) {
    val type = NotificationType.fromCode(item.type)
    val time = formatRelativeTime(item.createTime.orEmpty())

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onCardClick,
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 头像
        val avatarModifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onAvatarClick,
            )
        if (item.senderAvatar != null) {
            AsyncImage(
                model = item.senderAvatar,
                contentDescription = null,
                modifier = avatarModifier,
                contentScale = ContentScale.Crop,
            )
        } else {
            Box(
                modifier = avatarModifier.background(LimePrimaryPale),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = item.senderNickname?.take(1).orEmpty(),
                    fontSize = 18.sp,
                    color = LimePrimary,
                )
            }
        }

        Spacer(Modifier.width(12.dp))

        // 中间文本
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.senderNickname.orEmpty(),
                fontSize = 15.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "${type.actionText()} · $time",
                fontSize = 13.sp,
                lineHeight = 17.sp,
                color = LimeGray,
                modifier = Modifier.padding(top = 2.dp),
            )
            if (!item.content.isNullOrBlank()) {
                Text(
                    text = item.content,
                    fontSize = 14.sp,
                    lineHeight = 19.sp,
                    color = if (type == NotificationType.Reply) {
                        MaterialTheme.colorScheme.onBackground
                    } else {
                        LimeGray
                    },
                    maxLines = if (type == NotificationType.Reply) Int.MAX_VALUE else 1,
                    overflow = if (type == NotificationType.Reply) TextOverflow.Clip else TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            // 回复通知
            if (type == NotificationType.Reply && !item.replyToContent.isNullOrBlank()) {
                Text(
                    text = item.replyToContent,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = LimeGray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(LimeLightGray)
                        .padding(horizontal = 8.dp, vertical = 5.dp),
                )
            }
        }

        Spacer(Modifier.width(12.dp))

        // 笔记封面或关注按钮
        if (type == NotificationType.Follow) {
            val followState = relation?.toFollowActionState() ?: FollowActionState.Follow
            FollowButton(
                state = followState,
                onClick = if (followState == FollowActionState.Follow) onFollowClick else onUnfollowClick,
            )
        } else {
            NoteCover(url = item.noteCover)
        }
    }
}

@Composable
private fun NoteCover(url: String?) {
    val modifier = Modifier
        .size(48.dp)
        .clip(RoundedCornerShape(6.dp))
    if (url != null) {
        AsyncImage(
            model = url,
            contentDescription = null,
            modifier = modifier,
            contentScale = ContentScale.Crop,
        )
    } else {
        Box(
            modifier = modifier.background(LimeLightGray),
        )
    }
}

/// 通知类型对应的动作文案
private fun NotificationType.actionText(): String = when (this) {
    NotificationType.LikeNote -> "赞了你的笔记"
    NotificationType.Favorite -> "收藏了你的笔记"
    NotificationType.LikeComment -> "赞了你的评论"
    NotificationType.Follow -> "开始关注你了"
    NotificationType.Comment -> "评论了你的笔记"
    NotificationType.Reply -> "回复了你的评论"
}


private fun previewItem(
    id: Long,
    type: Int,
    nickname: String,
    content: String? = null,
    replyToContent: String? = null,
): NotificationData = NotificationData(
    id = id,
    type = type,
    noteId = 20,
    commentId = null,
    content = content,
    parentCommentId = null,
    replyToContent = replyToContent,
    noteCover = null,
    isRead = false,
    createTime = "2026-08-25T10:00:00",
    senderId = id,
    senderNickname = nickname,
    senderAvatar = null,
)






@Composable
private fun PreviewInbox(
    title: String,
    items: List<NotificationData>,
    relationFor: (Long) -> FollowRelation? = { null },
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .statusBarsPadding(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
        ) {
            IconButton(
                onClick = {},
                modifier = Modifier.align(Alignment.CenterStart),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "返回",
                    tint = MaterialTheme.colorScheme.onBackground,
                )
            }
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.align(Alignment.Center),
            )
        }
        HorizontalDivider(thickness = 0.5.dp, color = LimeLightGray)
        Column(Modifier.fillMaxWidth()) {
            items.forEach { item ->
                NotificationCard(
                    item = item,
                    relation = relationFor(item.id),
                    onCardClick = {},
                    onAvatarClick = {},
                    onFollowClick = {},
                    onUnfollowClick = {},
                )
                HorizontalDivider(
                    thickness = 0.5.dp,
                    color = LimeLightGray,
                    modifier = Modifier.padding(start = 72.dp),
                )
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 400, heightDp = 850)
@Composable
private fun LikesFavoritesInboxPreview() {
    LimeTheme {
        Surface {
            PreviewInbox(
                title = "收到的赞和收藏",
                items = listOf(
                    previewItem(1, 1, "小明"),
                    previewItem(2, 2, "小红"),
                    previewItem(3, 6, "唱跳 RAP", content = "这个配色绝了，学到了！"),
                ),
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 400, heightDp = 700)
@Composable
private fun FollowsInboxPreview() {
    LimeTheme {
        Surface {
            PreviewInbox(
                title = "新增关注",
                items = listOf(
                    previewItem(4, 5, "；李华"),
                    previewItem(5, 5, "hh"),
                    previewItem(6, 5, "hhh"),
                ),
                relationFor = { id ->
                    when (id) {
                        4L -> FollowRelation(following = false, followedBack = true) // 关注
                        5L -> FollowRelation(following = true, followedBack = true) // 互相关注
                        else -> FollowRelation(following = true, followedBack = false) // 已关注
                    }
                },
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 400, heightDp = 850)
@Composable
private fun CommentsInboxPreview() {
    LimeTheme {
        Surface {
            PreviewInbox(
                title = "收到的评论与回复",
                items = listOf(
                    previewItem(7, 3, "hhh", content = "拍得太好了吧，构图好棒！"),
                    previewItem(
                        8, 4, "开心就好",
                        content = "不知道～",
                        replyToContent = "Android 是什么",
                    ),
                ),
            )
        }
    }
}
