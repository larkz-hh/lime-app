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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import xyz.larkzhh.lime.R
import xyz.larkzhh.lime.domain.model.NotificationCategory
import xyz.larkzhh.lime.navigation.Screen
import xyz.larkzhh.lime.ui.theme.LimeGray
import xyz.larkzhh.lime.ui.theme.LimeLightGray
import xyz.larkzhh.lime.ui.theme.LimeTheme

/// 单个入口的视觉样式
private data class EntryStyle(
    val painterRes: Int?,
    val iconVector: ImageVector?,
    val background: Color,
    val tint: Color,
    val label: String,
)

private val entryStyles = listOf(
    EntryStyle(null, Icons.Filled.Favorite, Color(0xFFFBE9EC), Color(0xFFFF4D67), "赞和收藏"),
    EntryStyle(null, Icons.Filled.Person, Color(0xFFE7EFFE), Color(0xFF4A7DFF), "新增关注"),
    EntryStyle(R.drawable.ic_chat, null, Color(0xFFE3F5EC), Color(0xFF00B578), "评论和@"),
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
) {
    val unreadByCategory by viewModel.unreadByCategory.collectAsState()

    MessagePageContent(
        unreadByCategory = unreadByCategory,
        onEntryClick = { category ->
            navController.navigate(Screen.NotificationList.createRoute(category.routeKey))
        },
    )
}

@Composable
private fun MessagePageContent(
    unreadByCategory: Map<NotificationCategory, Int>,
    onEntryClick: (NotificationCategory) -> Unit,
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
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "消息",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
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

        HorizontalDivider(thickness = 0.5.dp, color = LimeLightGray)

        // 聊天列表占位
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    painter = painterResource(R.drawable.ic_chat),
                    contentDescription = null,
                    tint = LimeGray,
                    modifier = Modifier.size(44.dp),
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "聊天列表开发中",
                    fontSize = 15.sp,
                    color = LimeGray,
                )
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
                        contentDescription = style.label,
                        tint = style.tint,
                        modifier = Modifier.size(34.dp),
                    )
                } else {
                    Icon(
                        imageVector = style.iconVector!!,
                        contentDescription = style.label,
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
            text = style.label,
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
            .size(18.dp)
            .clip(CircleShape)
            .background(Color(0xFFFE2C55)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
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
                onEntryClick = {},
            )
        }
    }
}
