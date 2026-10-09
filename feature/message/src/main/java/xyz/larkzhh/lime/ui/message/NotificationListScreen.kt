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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import coil3.compose.AsyncImage
import xyz.larkzhh.lime.feature.message.R
import xyz.larkzhh.lime.data.network.model.NotificationData
import xyz.larkzhh.lime.domain.model.FollowActionState
import xyz.larkzhh.lime.domain.model.FollowRelation
import xyz.larkzhh.lime.domain.model.NotificationCategory
import xyz.larkzhh.lime.domain.model.NotificationType
import xyz.larkzhh.lime.domain.model.toFollowActionState
import xyz.larkzhh.lime.navigation.route.Screen
import xyz.larkzhh.lime.navigation.route.navigateToUserProfile
import xyz.larkzhh.lime.navigation.VideoOpener
import xyz.larkzhh.lime.ui.components.FollowButton
import xyz.larkzhh.lime.core.designsystem.components.SwipeActionItem
import xyz.larkzhh.lime.ui.components.UnfollowConfirmDialog
import xyz.larkzhh.lime.ui.theme.LimeGray
import xyz.larkzhh.lime.ui.theme.LimeTheme
import xyz.larkzhh.lime.ui.theme.LimeWhite
import xyz.larkzhh.lime.util.text.formatRelativeTime
import xyz.larkzhh.lime.core.designsystem.R as DesignSystemR

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationListScreen(
    navController: NavHostController,
    viewModel: NotificationListViewModel = hiltViewModel(),
) {
    val pageState by viewModel.pageState.collectAsState()
    val items by viewModel.items.collectAsState()
    val relations by viewModel.relations.collectAsState()
    val context = LocalContext.current

    var pendingUnfollow by remember { mutableStateOf<NotificationData?>(null) }

    val titleText = when (viewModel.category) {
        NotificationCategory.LikesFavorites -> stringResource(R.string.notif_title_likes)
        NotificationCategory.Follows -> stringResource(R.string.notif_title_follows)
        NotificationCategory.Comments -> stringResource(R.string.notif_title_comments)
    }
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
                    contentDescription = stringResource(DesignSystemR.string.back),
                    tint = MaterialTheme.colorScheme.onBackground,
                )
            }
            Text(
                text = titleText,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.align(Alignment.Center),
            )
        }
        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f))

        when {
            pageState.isInitialLoading && items.isEmpty() -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(28.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = LimeWhite,
                        strokeWidth = 2.dp,
                    )
                }
            }

            pageState.error != null && items.isEmpty() -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = pageState.error ?: stringResource(R.string.notif_load_failed),
                        color = LimeGray,
                        modifier = Modifier.clickable { viewModel.loadInitial() },
                    )
                }
            }

            items.isEmpty() -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = stringResource(R.string.notif_empty), color = LimeGray)
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
                        SwipeActionItem(
                            actionContent = {
                                Text(
                                    text = stringResource(DesignSystemR.string.delete),
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium,
                                )
                            },
                            onActionClick = { viewModel.delete(item) },
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
                                        else -> item.noteId?.let { noteId ->
                                            if (item.noteType == 2) {
                                                VideoOpener.open(
                                                    context,
                                                    noteId, Screen.VideoFeed.SOURCE_RECOMMENDATION)
                                            } else {
                                                navController.navigate(Screen.Detail.createRoute(noteId.toString()))
                                            }
                                        }
                                    }
                                },
                                onAvatarClick = {
                                    item.senderId?.let { navController.navigateToUserProfile(it) }
                                },
                                onFollowClick = { item.senderId?.let { viewModel.follow(it) } },
                                onUnfollowClick = { pendingUnfollow = item },
                                selfUserId = viewModel.currentUserId,
                            )
                        }
                        HorizontalDivider(
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f),
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
                                    color = MaterialTheme.colorScheme.primary,
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
    selfUserId: Long? = null,
) {
    val type = NotificationType.fromCode(item.type)
    val time = formatRelativeTime(item.createTime.orEmpty())
    val actionText = when (type) {
        NotificationType.LikeNote -> stringResource(R.string.notif_action_like_note)
        NotificationType.Favorite -> stringResource(R.string.notif_action_favorite_note)
        NotificationType.LikeComment -> stringResource(R.string.notif_action_like_comment)
        NotificationType.Follow -> stringResource(R.string.notif_action_follow)
        NotificationType.Comment -> stringResource(R.string.notif_action_comment_note)
        NotificationType.Reply -> stringResource(R.string.notif_action_reply_comment)
    }

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
                modifier = avatarModifier.background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = item.senderNickname?.take(1).orEmpty(),
                    fontSize = 18.sp,
                    color = LimeGray,
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
                text = "$actionText · $time",
                fontSize = 13.sp,
                lineHeight = 17.sp,
                color = LimeGray,
                modifier = Modifier.padding(top = 2.dp),
            )
            val content = item.content
            if (!content.isNullOrBlank()) {
                Text(
                    text = content,
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
            val replyToContent = item.replyToContent
            if (type == NotificationType.Reply && !replyToContent.isNullOrBlank()) {
                Text(
                    text = replyToContent,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = LimeGray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 8.dp, vertical = 5.dp),
                )
            }
        }

        Spacer(Modifier.width(12.dp))

        // 笔记封面或关注按钮
        if (type == NotificationType.Follow) {
            // 目标是自己时不渲染关注按钮
            val isSelf = selfUserId != null && item.senderId == selfUserId
            if (!isSelf) {
                val followState = relation?.toFollowActionState() ?: FollowActionState.Follow
                FollowButton(
                    state = followState,
                    onClick = if (followState == FollowActionState.Follow) onFollowClick else onUnfollowClick,
                )
            }
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
            modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
        )
    }
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
                    contentDescription = stringResource(DesignSystemR.string.back),
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
        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f))
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
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f),
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
                title = stringResource(R.string.notif_title_likes),
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
                title = stringResource(R.string.notif_title_follows),
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
                title = stringResource(R.string.notif_title_comments),
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
