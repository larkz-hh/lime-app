package xyz.larkzhh.lime.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VideoLabel
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.VideoLabel
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import xyz.larkzhh.lime.ui.detail.DetailScreen
import xyz.larkzhh.lime.ui.home.HomeScreen
import xyz.larkzhh.lime.ui.message.MessageScreen
import xyz.larkzhh.lime.ui.profile.ProfileScreen
import xyz.larkzhh.lime.ui.publish.PublishScreen
import xyz.larkzhh.lime.ui.video.VideoScreen

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
fun AppNavGraph() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showBottomBar = currentRoute != Screen.Detail.ROUTE && currentRoute != Screen.Publish.route

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                BottomNavBar(navController, currentRoute)
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) { HomeScreen(navController) }
            composable(Screen.Video.route) { VideoScreen(navController) }
            composable(Screen.Publish.route) { PublishScreen(navController) }
            composable(Screen.Message.route) { MessageScreen(navController) }
            composable(Screen.Profile.route) { ProfileScreen(navController) }
            composable(
                route = Screen.Detail.ROUTE,
                arguments = listOf(navArgument("noteId") { type = NavType.StringType })
            ) { backStackEntry ->
                DetailScreen(
                    navController = navController,
                    noteId = backStackEntry.arguments?.getString("noteId") ?: ""
                )
            }
        }
    }
}

@Composable
private fun BottomNavBar(navController: NavHostController, currentRoute: String?) {
    val items = listOf(
        BottomNavItem.Home,
        BottomNavItem.Video,
        BottomNavItem.Publish,
        BottomNavItem.Message,
        BottomNavItem.Profile,
    )

    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
        items.forEach { item ->
            if (item is BottomNavItem.Publish) {
                NavigationBarItem(
                    selected = false,
                    onClick = { navController.navigate(Screen.Publish.route) },
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
                NavigationBarItem(
                    selected = selected,
                    onClick = {
                        navController.navigate(item.screen.route) {
                            popUpTo(Screen.Home.route) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    icon = {
                        Icon(
                            imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                            contentDescription = item.label
                        )
                    },
                    label = { Text(item.label) }
                )
            }
        }
    }
}
