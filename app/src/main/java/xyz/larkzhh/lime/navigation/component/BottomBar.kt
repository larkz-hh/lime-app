package xyz.larkzhh.lime.navigation.component
import xyz.larkzhh.lime.core.designsystem.R as DesignSystemR
import xyz.larkzhh.lime.navigation.route.Screen

import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
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
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.exyte.animatednavbar.AnimatedNavigationBar
import com.exyte.animatednavbar.animation.balltrajectory.Parabolic
import com.exyte.animatednavbar.animation.indendshape.Height
import androidx.annotation.StringRes
import xyz.larkzhh.lime.R
import xyz.larkzhh.lime.ui.theme.AppTheme
import xyz.larkzhh.lime.ui.theme.ThemeOption
import xyz.larkzhh.lime.ui.theme.themeColorMap

private sealed class BottomNavItem(
    val screen: Screen,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    @StringRes val labelRes: Int?,
) {
    object Home : BottomNavItem(Screen.Home, Icons.Filled.Home, Icons.Outlined.Home, R.string.nav_home)
    object Video : BottomNavItem(Screen.Video, Icons.Filled.VideoLabel, Icons.Outlined.VideoLabel, R.string.nav_video)
    object Publish : BottomNavItem(Screen.Publish, Icons.Filled.Add, Icons.Filled.Add, null)
    object Message : BottomNavItem(Screen.Message, Icons.Filled.Notifications, Icons.Outlined.Notifications, DesignSystemR.string.nav_message)
    object Profile : BottomNavItem(Screen.Profile, Icons.Filled.Person, Icons.Outlined.Person, R.string.nav_profile)
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

    val selectedIndex = items.indexOfFirst { it.screen.route == currentRoute }.coerceAtLeast(0)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(BottomBarHeight)
            .background(MaterialTheme.colorScheme.primary),
    ) {
        AnimatedNavigationBar(
            modifier = Modifier
                .fillMaxWidth()
                .height(BottomBarHeight),
            selectedIndex = selectedIndex,
            barColor = MaterialTheme.colorScheme.surfaceContainerLowest,
            ballColor = currentBallColor(),// 小球主题色
            ballAnimation = Parabolic(spring(dampingRatio = 0.5f, stiffness = 180f)),// 小球动画
            indentAnimation = Height(tween(420), indentWidth = 56.dp, indentHeight = 12.dp),// 凹陷动画配合球速
        ) {
        items.forEach { item ->
            if (item is BottomNavItem.Publish) {
                CompactBottomNavItem(
                    selected = false,
                    onClick = {
                        if (!isLoggedIn) {
                            // 未登录，记录目标路由，跳转到登录页
                            onRequireLogin(Screen.Publish.route)
                        } else {
                            // 已登录，显示底部选择弹窗
                            onPublishClick()
                        }
                    },
                    label = null,
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = stringResource(R.string.nav_publish_desc),
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            } else {
                val itemLabel = item.labelRes?.let { stringResource(it) }
                val selected = currentRoute == item.screen.route
                val requiresAuth = item.screen.route in authRequiredScreens
                val onClick = {
                    if (requiresAuth && !isLoggedIn) {
                        onRequireLogin(item.screen.route)
                    } else if (item.screen.route == Screen.Home.route) {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Home.route) { saveState = true }// 保存其它 tab 的状态
                            launchSingleTop = true
                        }
                    } else {
                        navController.navigate(item.screen.route) {
                            popUpTo(Screen.Home.route) { saveState = true }// 保存状态
                            launchSingleTop = true
                            restoreState = true// 恢复之前状态
                        }
                    }
                }
                CompactBottomNavItem(
                    selected = selected,
                    onClick = onClick,
                    label = itemLabel,
                ) {
                    when (item) {
                        is BottomNavItem.Video -> {
                            Icon(
                                painter = painterResource(R.drawable.ic_video),
                                contentDescription = itemLabel,
                                modifier = Modifier.size(24.dp),
                            )
                        }

                        is BottomNavItem.Message -> {
                            Box {
                                Icon(
                                    painter = painterResource(DesignSystemR.drawable.ic_chat),
                                    contentDescription = itemLabel,
                                    modifier = Modifier.size(24.dp),
                                )
                                // 消息红点
                                if (messageUnread > 0) {
                                    MessageTabBadge(
                                        count = messageUnread,
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .offset(x = 12.dp, y = (-4).dp),
                                    )
                                }
                            }
                        }

                        else -> {
                            Box {
                                Icon(
                                    imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = itemLabel,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    }
}

/// 底部栏高度
internal val BottomBarHeight = 60.dp

/// 当前主题的底部栏小球色
@Composable
private fun currentBallColor(): Color =
    themeColorMap[ThemeOption.fromTag(AppTheme.currentTag())]?.ballPrimary ?: Color(0xFFA8E743)

/// 单个 tab
@Composable
private fun CompactBottomNavItem(
    selected: Boolean,
    onClick: () -> Unit,
    label: String?,
    icon: @Composable () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val colorScheme = MaterialTheme.colorScheme
    val contentColor = if (selected) colorScheme.primary else colorScheme.onSurfaceVariant

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(BottomBarHeight)
            .semantics(mergeDescendants = true) {}
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.Tab,
                interactionSource = interactionSource,
                indication = ripple(),
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (label != null) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Box(Modifier.clearAndSetSemantics {}) {
                    CompositionLocalProvider(LocalContentColor provides contentColor) {
                        icon()
                    }
                }
                Text(
                    text = label,
                    fontSize = 11.sp,
                    lineHeight = 14.sp,
                    color = contentColor,
                    maxLines = 1,
                )
            }
        } else {
            // 无文字 item
            CompositionLocalProvider(LocalContentColor provides contentColor) {
                icon()
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
