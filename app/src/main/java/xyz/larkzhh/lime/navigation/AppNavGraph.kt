package xyz.larkzhh.lime.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import kotlinx.coroutines.launch
import xyz.larkzhh.lime.ui.auth.LoginScreen
import xyz.larkzhh.lime.ui.auth.RegisterScreen
import xyz.larkzhh.lime.ui.auth.viewmodel.AuthViewModel
import xyz.larkzhh.lime.ui.detail.DetailScreen
import xyz.larkzhh.lime.ui.home.HomeScreen
import xyz.larkzhh.lime.ui.message.MessageScreen
import xyz.larkzhh.lime.ui.profile.EditProfileScreen
import xyz.larkzhh.lime.ui.profile.ProfileScreen
import xyz.larkzhh.lime.ui.publish.PhotoPickerScreen
import xyz.larkzhh.lime.ui.publish.PublishScreen
import xyz.larkzhh.lime.ui.publish.viewmodel.PublishViewModel
import xyz.larkzhh.lime.ui.qrscan.QrScanScreen
import xyz.larkzhh.lime.ui.theme.LimeWhite
import xyz.larkzhh.lime.ui.video.VideoScreen


private val bottomNavRoutes = setOf(
    Screen.Home.route,
    Screen.Video.route,
    Screen.Message.route,
    Screen.Profile.route,
)

/// 认证页面路由
private val authRoutes = setOf(Screen.Login.route, Screen.Register.route)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavGraph() {
    val authViewModel: AuthViewModel = hiltViewModel()
    val startDestination = Screen.Home.route
    var pendingRedirect by remember { mutableStateOf<String?>(null) }
    var showPublishSheet by remember { mutableStateOf(false) }
    var isFullScreenActive by remember { mutableStateOf(false) }// 是否全屏
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showBottomBar = currentRoute in bottomNavRoutes && !isFullScreenActive

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
                    onPublishClick = { showPublishSheet = true },
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = if (showBottomBar) Modifier.padding(bottom = innerPadding.calculateBottomPadding()) else Modifier
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
            composable(Screen.Message.route) { MessageScreen(navController) }
            composable(Screen.Profile.route) { ProfileScreen(navController) }
            composable(Screen.EditProfile.route) { EditProfileScreen(navController) }
            composable(Screen.QrScan.route) {
                DisposableEffect(Unit) {
                    isFullScreenActive = true
                    onDispose { isFullScreenActive = false }
                }
                QrScanScreen(navController)
            }
            composable(
                route = Screen.Detail.ROUTE,
                arguments = listOf(navArgument("noteId") { type = NavType.StringType })
            ) { backStackEntry ->
                DetailScreen(
                    navController = navController,
                    noteId = backStackEntry.arguments?.getString("noteId") ?: ""
                )
            }

            // 发布流程嵌套图，共享viewmodel
            navigation(
                startDestination = Screen.PhotoPicker.route,
                route = Screen.Publish.route,
            ) {
                composable(Screen.PhotoPicker.route) { entry ->
                    val parentEntry = remember(entry) {
                        navController.getBackStackEntry(Screen.Publish.route)
                    }
                    val viewModel: PublishViewModel = hiltViewModel(parentEntry)
                    PhotoPickerScreen(navController = navController, viewModel = viewModel)
                }// 生命周期与整个发布流程绑定
                composable(Screen.NotePublish.route) { entry ->
                    val parentEntry = remember(entry) {
                        navController.getBackStackEntry(Screen.Publish.route)
                    }
                    val viewModel: PublishViewModel = hiltViewModel(parentEntry)
                    PublishScreen(navController = navController, viewModel = viewModel)
                }
            }
        }
    }

    // 发布选择底部弹窗
    if (showPublishSheet) {
        ModalBottomSheet(
            onDismissRequest = { showPublishSheet = false },
            sheetState = sheetState,
            containerColor = LimeWhite
        ) {
            PublishBottomSheet(
                onAlbum = {
                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                        showPublishSheet = false
                        navController.navigate(Screen.Publish.route)
                    }
                },
                onCamera = {
                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                        showPublishSheet = false
                    }
                },
                onCancel = {
                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                        showPublishSheet = false
                    }
                },
            )
        }
    }
}