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
import androidx.compose.material.icons.filled.VideoLabel
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.VideoLabel
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import xyz.larkzhh.lime.ui.auth.LoginScreen
import xyz.larkzhh.lime.ui.auth.RegisterScreen
import xyz.larkzhh.lime.ui.auth.viewmodel.AuthViewModel
import xyz.larkzhh.lime.ui.detail.DetailScreen
import xyz.larkzhh.lime.ui.home.HomeScreen
import xyz.larkzhh.lime.ui.message.MessageScreen
import xyz.larkzhh.lime.ui.profile.ProfileScreen
import xyz.larkzhh.lime.ui.publish.PublishScreen
import xyz.larkzhh.lime.ui.video.VideoScreen

private val bottomNavRoutes = setOf(
    Screen.Home.route,
    Screen.Video.route,
    Screen.Message.route,
    Screen.Profile.route,
)

/// 认证页面路由
private val authRoutes = setOf(Screen.Login.route, Screen.Register.route)

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
    val authViewModel: AuthViewModel = hiltViewModel()
    val startDestination = Screen.Home.route
    var pendingRedirect by remember { mutableStateOf<String?>(null) }

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showBottomBar = currentRoute in bottomNavRoutes

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                BottomNavBar(
                    navController = navController,
                    currentRoute = currentRoute,
                    isLoggedIn = authViewModel.isLoggedIn(),
                    onRequireLogin = { targetRoute ->
                        pendingRedirect = targetRoute
                        if (currentRoute !in authRoutes) {
                            navController.navigate(Screen.Login.route)
                        }
                    },
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Login.route) {
                LoginScreen(
                    onLoginSuccess = {
                        val target = pendingRedirect ?: Screen.Home.route
                        pendingRedirect = null
                        navController.navigate(target) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    },
                    onNavigateToRegister = {
                        navController.navigate(Screen.Register.route)
                    },
                    onBack = { navController.popBackStack() },
                )
            }
            composable(Screen.Register.route) {
                RegisterScreen(
                    onRegisterSuccess = {
                        val target = pendingRedirect ?: Screen.Home.route
                        pendingRedirect = null
                        navController.navigate(target) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    },
                    onNavigateToLogin = {
                        navController.popBackStack()
                    },
                )
            }
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
private fun BottomNavBar(
    navController: NavHostController,
    currentRoute: String?,
    isLoggedIn: Boolean,
    onRequireLogin: (String) -> Unit,
) {
    val items = listOf(
        BottomNavItem.Home,
        BottomNavItem.Video,
        BottomNavItem.Publish,
        BottomNavItem.Message,
        BottomNavItem.Profile,
    )

    val authRequiredScreens = setOf(Screen.Message.route, Screen.Profile.route)

    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
        items.forEach { item ->
            if (item is BottomNavItem.Publish) {
                NavigationBarItem(
                    selected = false,
                    onClick = {
                        if (!isLoggedIn) {
                            onRequireLogin(Screen.Publish.route)
                        } else {
                            navController.navigate(Screen.Publish.route)
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
                NavigationBarItem(
                    selected = selected,
                    onClick = {
                        if (requiresAuth && !isLoggedIn) {
                            onRequireLogin(item.screen.route)
                        } else {
                            navController.navigate(item.screen.route) {
                                popUpTo(Screen.Home.route) { saveState = true }// 保存状态
                                launchSingleTop = true
                                restoreState = true// 恢复之前状态
                            }
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
