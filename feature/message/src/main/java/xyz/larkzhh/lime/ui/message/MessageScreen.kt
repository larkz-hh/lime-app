package xyz.larkzhh.lime.ui.message

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import xyz.larkzhh.lime.core.designsystem.R as DesignSystemR
import xyz.larkzhh.lime.feature.message.R
import xyz.larkzhh.lime.domain.model.ImConversation
import xyz.larkzhh.lime.domain.model.NotificationCategory
import xyz.larkzhh.lime.navigation.route.Screen
import xyz.larkzhh.lime.ui.components.LimeAlertDialog
import xyz.larkzhh.lime.ui.components.SwipeActionItem
import xyz.larkzhh.lime.ui.im.viewmodel.ImConversationViewModel
import xyz.larkzhh.lime.util.text.formatConversationTime
import xyz.larkzhh.lime.ui.theme.LimeGray
import xyz.larkzhh.lime.ui.theme.LimeLightGray
import xyz.larkzhh.lime.ui.theme.LimeTheme

/// 单个入口的视觉样式
private data class EntryStyle(
    val painterRes: Int?,
    val iconVector: ImageVector?,
    val background: Color,
    val tint: Color,
    val labelRes: Int,
)

private val entryStyles = listOf(
    EntryStyle(null, Icons.Filled.Favorite, Color(0xFFFBE9EC), Color(0xFFFF4D67), R.string.msg_entry_likes),
    EntryStyle(null, Icons.Filled.Person, Color(0xFFE7EFFE), Color(0xFF4A7DFF), R.string.msg_entry_follows),
    EntryStyle(DesignSystemR.drawable.ic_chat, null, Color(0xFFE3F5EC), Color(0xFF00B578), R.string.msg_entry_comments),
)

private val categories = listOf(
    NotificationCategory.LikesFavorites,
    NotificationCategory.Follows,
    NotificationCategory.Comments,
)

@Composable
fun MessageScreen(
    navController: NavHostController,
    viewModel: MessageViewModel = hiltViewModel(),
    imConversationViewModel: ImConversationViewModel = hiltViewModel(),
) {
    val unreadByCategory by viewModel.unreadByCategory.collectAsState()
    val imState by imConversationViewModel.state.collectAsState()
    var deleteTarget by remember { mutableStateOf<ImConversation?>(null) }

    // 页面重新可见
    LaunchedEffect(Unit) {
        imConversationViewModel.refresh()
    }

    MessagePageContent(
        unreadByCategory = unreadByCategory,
        conversations = imState.conversations,
        onEntryClick = { category ->
            navController.navigate(Screen.NotificationList.createRoute(category.routeKey))
        },
        onConversationClick = { conversationId ->
            navController.navigate(Screen.ImChat.createRoute(conversationId)) { launchSingleTop = true }
        },
        onConversationDelete = { conv -> deleteTarget = conv },
        onCreateGroup = { navController.navigate(Screen.CreateGroup.route) },
        onAddFriend = { navController.navigate(Screen.AddFriend.route) },
        onScan = { navController.navigate(Screen.QrScan.route) },
    )

    // 删除会话确认弹窗
    deleteTarget?.let { target ->
        val isGroup = target.conversationId.startsWith("group_")
        LimeAlertDialog(
            title = stringResource(R.string.msg_delete_conversation),
            text = if (isGroup) {
                stringResource(R.string.msg_delete_group_hint)
            } else {
                stringResource(R.string.msg_delete_conversation_hint)
            },
            firstButtonText = stringResource(DesignSystemR.string.cancel),
            secondButtonText = stringResource(DesignSystemR.string.delete),
            secondButtonColor = Color(0xFFFE2C55),
            onFirstButtonClick = { deleteTarget = null },
            onSecondButtonClick = {
                deleteTarget = null
                imConversationViewModel.deleteConversation(target.conversationId)
            },
            onDismissRequest = { deleteTarget = null },
        )
    }
}

@Composable
private fun MessagePageContent(
    unreadByCategory: Map<NotificationCategory, Int>,
    conversations: List<ImConversation>,
    onEntryClick: (NotificationCategory) -> Unit,
    onConversationClick: (String) -> Unit,
    onConversationDelete: (ImConversation) -> Unit = {},
    onCreateGroup: () -> Unit = {},
    onAddFriend: () -> Unit = {},
    onScan: () -> Unit = {},
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .statusBarsPadding(),
    ) {
        // 顶部栏
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
        ) {
            var showAddMenu by remember { mutableStateOf(false) }
            Text(
                text = stringResource(DesignSystemR.string.nav_message),
                modifier = Modifier.align(Alignment.Center),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            // 右弹出菜单
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 4.dp),
            ) {
                IconButton(onClick = { showAddMenu = true }) {
                    Icon(
                        Icons.Filled.Add,
                        contentDescription = stringResource(R.string.msg_more),
                        tint = MaterialTheme.colorScheme.onBackground,
                    )
                }
                DropdownMenu(
                    expanded = showAddMenu,
                    onDismissRequest = { showAddMenu = false },
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(DesignSystemR.string.msg_create_group), color = MaterialTheme.colorScheme.onSurface) },
                        onClick = { showAddMenu = false; onCreateGroup() },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.msg_add_friend), color = MaterialTheme.colorScheme.onSurface) },
                        onClick = { showAddMenu = false; onAddFriend() },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.msg_scan), color = MaterialTheme.colorScheme.onSurface) },
                        onClick = { showAddMenu = false; onScan() },
                    )
                }
            }
        }

        // 三个入口
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceAround,
        ) {
            categories.forEachIndexed { index, category ->
                NotificationEntry(
                    style = entryStyles[index],
                    unread = unreadByCategory[category] ?: 0,
                    onClick = { onEntryClick(category) },
                )
            }
        }

        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f))

        // 私信会话列表
        if (conversations.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        painter = painterResource(DesignSystemR.drawable.ic_chat),
                        contentDescription = null,
                        tint = LimeGray,
                        modifier = Modifier.size(44.dp),
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = stringResource(R.string.msg_empty),
                        fontSize = 15.sp,
                        color = LimeGray,
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) {
                items(conversations, key = { it.conversationId }) { conversation ->
                    SwipeActionItem(
                        actionContent = {
                            Text(
                                text = stringResource(DesignSystemR.string.delete),
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                            )
                        },
                        onActionClick = { onConversationDelete(conversation) },
                    ) {
                        ConversationListItem(
                            conversation = conversation,
                            onClick = { onConversationClick(conversation.conversationId) },
                        )
                    }
                }
            }
        }
    }
}

/// 私信会话项
@Composable
private fun ConversationListItem(
    conversation: ImConversation,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClick,
            )
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 头像
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            if (!conversation.faceUrl.isNullOrBlank()) {
                AsyncImage(
                    model = conversation.faceUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Text(
                    text = conversation.showName.take(1).ifBlank { "?" },
                    color = LimeGray,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        Spacer(Modifier.width(12.dp))

        // 昵称与最后消息
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = conversation.showName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = conversation.lastMessageText.ifBlank { stringResource(R.string.msg_start_chat) },
                fontSize = 13.sp,
                color = LimeGray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Spacer(Modifier.width(8.dp))

        // 时间、未读
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = formatConversationTime(conversation.timestamp),
                fontSize = 11.sp,
                color = LimeGray,
            )
            if (conversation.unreadCount > 0) {
                Spacer(Modifier.height(4.dp))
                UnreadBadge(count = conversation.unreadCount)
            }
        }
    }
}

/// 单个入口
@Composable
private fun NotificationEntry(
    style: EntryStyle,
    unread: Int,
    onClick: () -> Unit,
) {
    val label = stringResource(style.labelRes)
    Column(
        modifier = Modifier
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClick,
            )
            .padding(horizontal = 4.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(style.background),
                contentAlignment = Alignment.Center,
            ) {
                if (style.painterRes != null) {
                    Icon(
                        painter = painterResource(style.painterRes),
                        contentDescription = label,
                        tint = style.tint,
                        modifier = Modifier.size(34.dp),
                    )
                } else {
                    Icon(
                        imageVector = style.iconVector!!,
                        contentDescription = label,
                        tint = style.tint,
                        modifier = Modifier.size(34.dp),
                    )
                }
            }
            if (unread > 0) {
                UnreadBadge(
                    count = unread,
                    modifier = Modifier.align(Alignment.TopEnd),
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = label,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

@Composable
private fun UnreadBadge(
    count: Int,
    modifier: Modifier = Modifier,
) {
    val text = if (count > 99) "99+" else count.toString()
    Box(
        modifier = modifier
            .height(18.dp)
            .widthIn(min = 18.dp)
            .clip(RoundedCornerShape(percent = 50))
            .background(Color(0xFFFE2C55))
            .padding(horizontal = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            fontSize = 10.sp,
            lineHeight = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            maxLines = 1,
            textAlign = TextAlign.Center,
        )
    }
}

@Preview(showBackground = true, widthDp = 400, heightDp = 800)
@Composable
private fun MessagePagePreview() {
    LimeTheme {
        Surface {
            MessagePageContent(
                unreadByCategory = mapOf(
                    NotificationCategory.LikesFavorites to 3,
                    NotificationCategory.Follows to 1,
                    NotificationCategory.Comments to 0,
                ),
                conversations = emptyList(),
                onEntryClick = {},
                onConversationClick = {},
            )
        }
    }
}
