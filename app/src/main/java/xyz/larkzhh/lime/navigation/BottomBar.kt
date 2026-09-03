package xyz.larkzhh.lime.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.VideoLabel
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.VideoLabel
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import xyz.larkzhh.lime.R

private sealed class BottomNavItem(
    val screen: Screen,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val label: String,
) {
    object Home : BottomNavItem(Screen.Home, Icons.Filled.Home, Icons.Outlined.Home, "首页")
    object Video : BottomNavItem(Screen.Video, Icons.Filled.VideoLabel, Icons.Outlined.VideoLabel, "视频")
    object Publish : BottomNavItem(Screen.Publish, Icons.Filled.Add, Icons.Filled.Add, "")
    object Message : BottomNavItem(Screen.Message, Icons.Filled.Notifications, Icons.Outlined.Notifications, "消息")
    object Profile : BottomNavItem(Screen.Profile, Icons.Filled.Person, Icons.Outlined.Person, "我")
}

@Composable
fun BottomNavBar(
    navController: NavHostController,
    currentRoute: String?,
    isLoggedIn: Boolean,
    onRequireLogin: (String) -> Unit,
    onPublishClick: () -> Unit,
    messageUnread: Int = 0,
) {
    val items = listOf(
        BottomNavItem.Home,
        BottomNavItem.Video,
        BottomNavItem.Publish,
        BottomNavItem.Message,
        BottomNavItem.Profile,
    )

    val authRequiredScreens = setOf(Screen.Message.route, Screen.Profile.route)

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        windowInsets = WindowInsets(0),
    ) {
        items.forEach { item ->
            if (item is BottomNavItem.Publish) {
                NavigationBarItem(
                    selected = false,
                    onClick = {
                        if (!isLoggedIn) {
                            // 未登录：记录目标路由，跳转到登录页
                            onRequireLogin(Screen.Publish.route)
                        } else {
                            // 已登录，显示底部选择弹窗
                            onPublishClick()
                        }
                    },
                    icon = {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = "发布",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    },
                    label = null,
                    alwaysShowLabel = false
                )
            } else {
                val selected = currentRoute == item.screen.route
                val requiresAuth = item.screen.route in authRequiredScreens
                val onClick = {
                    if (requiresAuth && !isLoggedIn) {
                        onRequireLogin(item.screen.route)
                    } else {
                        navController.navigate(item.screen.route) {
                            popUpTo(Screen.Home.route) { saveState = true }// 保存状态
                            launchSingleTop = true
                            restoreState = true// 恢复之前状态
                        }
                    }
                }
                if (item is BottomNavItem.Video) {
                    NavigationBarItem(
                        selected = selected,
                        onClick = onClick,
                        icon = {
                            Icon(
                                painter = painterResource(R.drawable.ic_video),
                                contentDescription = item.label,
                                modifier = Modifier.size(24.dp),
                            )
                        },
                        label = { Text(item.label) },
                    )
                } else {
                    NavigationBarItem(
                        selected = selected,
                        onClick = onClick,
                        icon = {
                            Box {
                                Icon(
                                    imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = item.label,
                                )
                                // 消息红点
                                if (item is BottomNavItem.Message && messageUnread > 0) {
                                    MessageTabBadge(
                                        count = messageUnread,
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .offset(x = 10.dp, y = (-4).dp),
                                    )
                                }
                            }
                        },
                        label = { Text(item.label) },
                    )
                }
            }
        }
    }
}

/// 消息红点
@Composable
private fun MessageTabBadge(
    count: Int,
    modifier: Modifier = Modifier,
) {
    val text = if (count > 99) "99+" else count.toString()
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(9.dp))
            .background(Color(0xFFFE2C55))
            .padding(horizontal = 4.dp, vertical = 1.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            fontSize = 9.sp,
            lineHeight = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
        )
    }
}
