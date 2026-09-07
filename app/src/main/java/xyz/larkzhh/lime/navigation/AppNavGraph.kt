package xyz.larkzhh.lime.navigation

import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavType
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import kotlinx.coroutines.launch
import xyz.larkzhh.lime.R
import xyz.larkzhh.lime.ui.ai.AiChatScreen
import xyz.larkzhh.lime.ui.about.AboutScreen
import xyz.larkzhh.lime.ui.auth.LoginScreen
import xyz.larkzhh.lime.ui.auth.RegisterScreen
import xyz.larkzhh.lime.ui.auth.viewmodel.AuthViewModel
import xyz.larkzhh.lime.util.ForceLogoutBus
import xyz.larkzhh.lime.util.showToast
import xyz.larkzhh.lime.ui.components.ForceLogoutDialog
import xyz.larkzhh.lime.ui.detail.comment.CommentPhotoPickerScreen
import xyz.larkzhh.lime.ui.draft.DraftBoxOverlay
import xyz.larkzhh.lime.ui.detail.DetailScreen
import xyz.larkzhh.lime.ui.follow.FollowListScreen
import xyz.larkzhh.lime.ui.friend.AddFriendScreen
import xyz.larkzhh.lime.ui.group.CreateGroupScreen
import xyz.larkzhh.lime.ui.group.GroupListScreen
import xyz.larkzhh.lime.ui.group.GroupManageScreen
import xyz.larkzhh.lime.ui.home.HomeScreen
import xyz.larkzhh.lime.ui.im.ChatScreen
import xyz.larkzhh.lime.ui.im.viewmodel.ImViewModel
import xyz.larkzhh.lime.ui.message.MessageScreen
import xyz.larkzhh.lime.ui.message.MessageViewModel
import xyz.larkzhh.lime.ui.message.NotificationListScreen
import xyz.larkzhh.lime.ui.profile.edit.EditProfileScreen
import xyz.larkzhh.lime.ui.profile.ProfileScreen
import xyz.larkzhh.lime.ui.profile.account.AccountPrivacyScreen
import xyz.larkzhh.lime.ui.profile.components.ProfileDrawerContent
import xyz.larkzhh.lime.ui.profile.history.BrowseHistoryScreen
import xyz.larkzhh.lime.ui.publish.PhotoPickerScreen
import xyz.larkzhh.lime.ui.publish.PublishScreen
import xyz.larkzhh.lime.ui.publish.VideoPublishScreen
import xyz.larkzhh.lime.ui.publish.CoverPickerScreen
import xyz.larkzhh.lime.ui.publish.viewmodel.PublishViewModel
import xyz.larkzhh.lime.ui.publish.viewmodel.VideoPublishViewModel
import xyz.larkzhh.lime.ui.qrscan.QrScanScreen
import xyz.larkzhh.lime.ui.search.SearchScreen
import xyz.larkzhh.lime.ui.settings.GeneralSettingsScreen
import xyz.larkzhh.lime.ui.translate.TranslatePackScreen
import xyz.larkzhh.lime.ui.video.feed.VideoFeedScreen
import xyz.larkzhh.lime.ui.video.player.VideoPlayerManager


private val bottomNavRoutes = setOf(
    Screen.Home.route,
    Screen.Video.route,
    Screen.Message.route,
    Screen.Profile.route,
)

/// 认证页面路由
private val authRoutes = setOf(Screen.Login.route, Screen.Register.route)

/// 需要右滑预测性返回水平滑出、滑入的页面
private val swipeBackRoutes =
    setOf(Screen.Detail.ROUTE, Screen.UserProfile.ROUTE, Screen.Search.ROUTE)

@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavGraph(
    playerManager: VideoPlayerManager,
    shortcutAction: String? = null,
    shortcutKeyword: String? = null,
    shortcutConversationId: String? = null,
    onShortcutHandled: () -> Unit = {},
) {
    val authViewModel: AuthViewModel = hiltViewModel()
    val messageViewModel: MessageViewModel = hiltViewModel()
    val imViewModel: ImViewModel = hiltViewModel()
    val combinedUnread by messageViewModel.combinedUnread.collectAsState()
    val startDestination = Screen.Home.route
    var pendingRedirect by remember { mutableStateOf<String?>(null) }
    var loginReturnToPrevious by remember { mutableStateOf(false) }// 操作登录拦截
    var showPublishSheet by remember { mutableStateOf(false) }
    var isFullScreenActive by remember { mutableStateOf(false) }// 是否全屏
    var videoTabFullscreen by remember { mutableStateOf(false) }// 视频 tab 横屏全屏
    var videoTabOverlay by remember { mutableStateOf(false) }// 视频 tab 底部浮层打开
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    var showTranslatePack by remember { mutableStateOf(false) }
    var showDraftBox by remember { mutableStateOf(false) }
    var showAccountPrivacy by remember { mutableStateOf(false) }
    var showGeneralSettings by remember { mutableStateOf(false) }
    var showAbout by remember { mutableStateOf(false) }
    var forceLogout by rememberSaveable { mutableStateOf(false) }// 重建后弹窗状态保留

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showBottomBar =
        currentRoute in bottomNavRoutes && !isFullScreenActive && !videoTabFullscreen && !videoTabOverlay

    // 操作登录拦截
    remember {
        val gate: (String?) -> Boolean = { target ->
            if (authViewModel.isLoggedIn()) {
                false
            } else {
                pendingRedirect = target
                loginReturnToPrevious = target == null
                val topRoute = navController.currentBackStackEntry?.destination?.route
                if (topRoute !in authRoutes) {
                    navController.navigate(Screen.Login.route)
                }
                true
            }
        }
        LoginGate.onRequireLogin = gate
        gate
    }

    // 强制下线本地清理
    fun cleanupForceLogout() {
        showTranslatePack = false
        showDraftBox = false
        showAccountPrivacy = false
        scope.launch { drawerState.close() }
        authViewModel.logout()
        imViewModel.logout()
        messageViewModel.onLoggedOut()
    }

    // 强制下线弹窗
    LaunchedEffect(Unit) {
        ForceLogoutBus.events.collect {
            forceLogout = true
            runCatching { cleanupForceLogout() }
        }
    }

    // 长按图标快捷入口 / 通知点击跳转
    LaunchedEffect(shortcutAction) {
        val route = when (shortcutAction) {
            ShortcutActions.SEARCH -> Screen.Search.BASE_ROUTE
            ShortcutActions.SEARCH_KEYWORD -> Screen.Search.createRoute(shortcutKeyword.orEmpty())
            ShortcutActions.AI_CHAT -> Screen.AiChat.createRoute(Screen.AiChat.NEW_CONVERSATION)
            ShortcutActions.QR_SCAN -> Screen.QrScan.route
            // 系统通知点击进消息页
            ShortcutActions.OPEN_MESSAGE -> Screen.Message.route
            else -> null
        }
        val imConversationId = if (shortcutAction == ShortcutActions.OPEN_IM_CHAT) shortcutConversationId else null
        if (imConversationId != null) {
            navController.navigate(Screen.ImChat.createRoute(imConversationId)) { launchSingleTop = true }
            onShortcutHandled()
        } else if (route != null) {
            if (route == Screen.Message.route) {
                navController.navigate(Screen.Message.route) {
                    popUpTo(Screen.Home.route) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            } else {
                navController.navigate(route) { launchSingleTop = true }
            }
            onShortcutHandled()
        }
    }


    LaunchedEffect(currentRoute) {
        SwipeBackNavState.suppressPopAnim = false
        SwipeBackNavState.gestureDrivenPop = false
    }

    LaunchedEffect(currentRoute) {
        if (currentRoute != Screen.Profile.route) {
            scope.launch { drawerState.close() }
            showTranslatePack = false
            showDraftBox = false
            showAccountPrivacy = false
            showGeneralSettings = false
            showAbout = false
        }
    }

    // 登录后进入主界面同步未读红点并保持 SSE并预登录 IM
    LaunchedEffect(currentRoute) {
        when (currentRoute) {
            in authRoutes -> messageViewModel.onLoggedOut()// 登录注册时通知
            in bottomNavRoutes if authViewModel.isLoggedIn() -> {
                messageViewModel.sync()
                imViewModel.ensureImLogin()
            }
        }
    }

    // 双击返回退出
    var lastExitBackTime by remember { mutableLongStateOf(0L) }
    val exitContext = LocalContext.current
    val exitHintText = stringResource(R.string.exit_again_hint)
    BackHandler(enabled = showBottomBar) {
        val now = System.currentTimeMillis()
        if (now - lastExitBackTime <= 2000) {
            (exitContext as? Activity)?.finish()
        } else {
            lastExitBackTime = now
            exitHintText.showToast(exitContext)
        }
    }

    Box {
        // 抽屉栏
        ModalNavigationDrawer(
            drawerState = drawerState,
            gesturesEnabled = currentRoute == Screen.Profile.route,
            drawerContent = {
                ProfileDrawerContent(
                    onTranslateClick = { showTranslatePack = true },
                    onDraftsClick = { showDraftBox = true },
                    onAccountPrivacyClick = { showAccountPrivacy = true },
                    onGeneralClick = { showGeneralSettings = true },
                    onAboutClick = { showAbout = true },
                )
            },
        ) {
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
                            // 消息 tab 红点 = 站内通知 + IM 未读合计
                            messageUnread = if (authViewModel.isLoggedIn()) combinedUnread else 0,
                        )
                    }
                }
            ) { innerPadding ->
                NavHost(
                    navController = navController,
                    startDestination = startDestination,
                    modifier = if (showBottomBar) Modifier.padding(bottom = innerPadding.calculateBottomPadding()) else Modifier,
                    popExitTransition = {
                        when {
                            SwipeBackNavState.suppressPopAnim -> ExitTransition.None
                            // 自定义右滑手势驱动的pop才滑出
                            SwipeBackNavState.gestureDrivenPop &&
                                    initialState.destination.route in swipeBackRoutes ->
                                slideOutHorizontally(
                                    animationSpec = tween(220),
                                    targetOffsetX = { it })

                            else -> ExitTransition.None
                        }
                    },
                    popEnterTransition = {
                        when {
                            SwipeBackNavState.suppressPopAnim -> EnterTransition.None
                            SwipeBackNavState.gestureDrivenPop &&
                                    initialState.destination.route in swipeBackRoutes ->
                                slideInHorizontally(
                                    animationSpec = tween(220),
                                    initialOffsetX = { -it / 4 })

                            else -> EnterTransition.None
                        }
                    },
                ) {
                    composable(Screen.Login.route) {
                        LoginScreen(
                            onLoginSuccess = {
                                val target = pendingRedirect
                                pendingRedirect = null
                                when {
                                    // 操作拦截
                                    loginReturnToPrevious -> {
                                        loginReturnToPrevious = false
                                        navController.popBackStack()
                                    }
                                    target != null -> navController.navigate(target) {
                                        popUpTo(Screen.Login.route) { inclusive = true }
                                    }
                                    else -> navController.navigate(Screen.Home.route) {
                                        popUpTo(Screen.Login.route) { inclusive = true }
                                    }
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
                    composable(Screen.Home.route) { entry ->
                        ScrimBox(entry.id) {
                            HomeScreen(
                                navController
                            )
                        }
                    }
                    composable(Screen.Video.route) { entry ->
                        ScrimBox(entry.id) {
                            VideoFeedScreen(
                                navController = navController,
                                playerManager = playerManager,
                                onFullscreenChange = { videoTabFullscreen = it },
                                onOverlayChange = { videoTabOverlay = it },
                            )
                        }
                    }
                    composable(Screen.Message.route) { entry ->
                        ScrimBox(entry.id) {
                            MessageScreen(
                                navController,
                                viewModel = messageViewModel,
                            )
                        }
                    }
                    composable(Screen.Profile.route) { entry ->
                        ScrimBox(entry.id) {
                            ProfileScreen(
                                navController = navController,
                                onOpenDrawer = { scope.launch { drawerState.open() } },
                            )
                        }
                    }
                    composable(
                        route = Screen.UserProfile.ROUTE,
                        arguments = listOf(navArgument("userId") { type = NavType.LongType }),
                        enterTransition = {
                            if (SwipeBackNavState.suppressForwardEnter) {
                                EnterTransition.None
                            } else {
                                slideInHorizontally(initialOffsetX = { it })// 非手势从右侧滑入
                            }
                        },
                        popEnterTransition = {
                            when {
                                SwipeBackNavState.suppressPopAnim -> EnterTransition.None
                                else -> slideInHorizontally(
                                    animationSpec = tween(220),
                                    initialOffsetX = { -it / 4 })
                            }
                        },
                        // 返回时右侧滑出
                        popExitTransition = {
                            when {
                                SwipeBackNavState.suppressPopAnim -> ExitTransition.None
                                else -> slideOutHorizontally(
                                    animationSpec = tween(220),
                                    targetOffsetX = { it })
                            }
                        },
                    ) { backStackEntry ->
                        val userId =
                            backStackEntry.arguments?.getLong("userId") ?: return@composable
                        // 笔记作者用户界面，复用跨返回栈会话
                        val session = AuthorProfileStore.get(userId)
                        ScrimBox(backStackEntry.id) {
                            if (session != null) {
                                ProfileScreen(
                                    navController = navController,
                                    userId = userId,
                                    viewModel = session.profileViewModel,
                                    notesViewModel = session.notesViewModel,
                                    session = session,
                                )
                            } else {
                                ProfileScreen(navController = navController, userId = userId)
                            }
                        }
                    }
                    composable(
                        route = Screen.Search.route,
                        arguments = listOf(
                            navArgument(Screen.Search.ARG_QUERY) {
                                type = NavType.StringType
                                defaultValue = ""
                            },
                        ),
                        // 前进时从右侧滑入
                        enterTransition = {
                            if (SwipeBackNavState.suppressForwardEnter) {
                                EnterTransition.None
                            } else {
                                slideInHorizontally(initialOffsetX = { it })// 从右侧滑入
                            }
                        },
                    ) { entry ->
                        ScrimBox(entry.id) { SearchScreen(navController) }
                    }
                    composable(Screen.EditProfile.route) { EditProfileScreen(navController) }
                    composable(Screen.QrScan.route) {
                        DisposableEffect(Unit) {
                            isFullScreenActive = true
                            onDispose { isFullScreenActive = false }
                        }
                        QrScanScreen(navController)
                    }
                    composable(
                        route = Screen.AddFriend.route,
                        enterTransition = {
                            slideInHorizontally(animationSpec = tween(280), initialOffsetX = { it })
                        },
                        exitTransition = {
                            slideOutHorizontally(animationSpec = tween(280), targetOffsetX = { it })
                        },
                        popEnterTransition = {
                            slideInHorizontally(animationSpec = tween(280), initialOffsetX = { -it })
                        },
                        popExitTransition = {
                            slideOutHorizontally(animationSpec = tween(280), targetOffsetX = { it })
                        },
                    ) {
                        AddFriendScreen(navController)
                    }
                    composable(
                        route = Screen.CreateGroup.route,
                        enterTransition = {
                            slideInHorizontally(animationSpec = tween(280), initialOffsetX = { it })
                        },
                        exitTransition = {
                            slideOutHorizontally(animationSpec = tween(280), targetOffsetX = { it })
                        },
                        popEnterTransition = {
                            slideInHorizontally(animationSpec = tween(280), initialOffsetX = { -it })
                        },
                        popExitTransition = {
                            slideOutHorizontally(animationSpec = tween(280), targetOffsetX = { it })
                        },
                    ) {
                        CreateGroupScreen(navController)
                    }
                    composable(
                        route = Screen.GroupList.route,
                        enterTransition = {
                            slideInHorizontally(animationSpec = tween(280), initialOffsetX = { it })
                        },
                        exitTransition = {
                            slideOutHorizontally(animationSpec = tween(280), targetOffsetX = { it })
                        },
                        popEnterTransition = {
                            slideInHorizontally(animationSpec = tween(280), initialOffsetX = { -it })
                        },
                        popExitTransition = {
                            slideOutHorizontally(animationSpec = tween(280), targetOffsetX = { it })
                        },
                    ) {
                        GroupListScreen(navController)
                    }
                    composable(
                        route = Screen.GroupManage.ROUTE,
                        arguments = listOf(navArgument("groupId") { type = NavType.StringType }),
                        enterTransition = {
                            slideInHorizontally(animationSpec = tween(280), initialOffsetX = { it })
                        },
                        exitTransition = {
                            slideOutHorizontally(animationSpec = tween(280), targetOffsetX = { it })
                        },
                        popEnterTransition = {
                            slideInHorizontally(animationSpec = tween(280), initialOffsetX = { -it })
                        },
                        popExitTransition = {
                            slideOutHorizontally(animationSpec = tween(280), targetOffsetX = { it })
                        },
                    ) { backStackEntry ->
                        val groupId = backStackEntry.arguments?.getString("groupId") ?: return@composable
                        GroupManageScreen(groupId = groupId, navController = navController)
                    }
                    composable(Screen.BrowseHistory.route) { BrowseHistoryScreen(navController) }
                    composable(
                        route = Screen.FollowList.ROUTE,
                        arguments = listOf(
                            navArgument("userId") { type = NavType.LongType },
                            navArgument("tab") {
                                type = NavType.StringType
                                defaultValue = Screen.FollowList.TAB_FOLLOWING
                            },
                        ),
                        enterTransition = {
                            if (SwipeBackNavState.suppressForwardEnter) {
                                EnterTransition.None
                            } else {
                                slideInHorizontally(initialOffsetX = { it })
                            }
                        },
                        popEnterTransition = {
                            if (SwipeBackNavState.suppressPopAnim) EnterTransition.None
                            else slideInHorizontally(
                                animationSpec = tween(220),
                                initialOffsetX = { -it / 4 })
                        },
                        popExitTransition = {
                            if (SwipeBackNavState.suppressPopAnim) ExitTransition.None
                            else slideOutHorizontally(
                                animationSpec = tween(220),
                                targetOffsetX = { it })
                        },
                    ) {
                        FollowListScreen(navController)
                    }
                    composable(
                        route = Screen.NotificationList.ROUTE,
                        arguments = listOf(
                            navArgument(Screen.NotificationList.ARG_TYPE) {
                                type = NavType.StringType
                                defaultValue = "likes"
                            },
                        ),
                        enterTransition = {
                            if (SwipeBackNavState.suppressForwardEnter) {
                                EnterTransition.None
                            } else {
                                slideInHorizontally(initialOffsetX = { it })
                            }
                        },
                        popEnterTransition = {
                            if (SwipeBackNavState.suppressPopAnim) EnterTransition.None
                            else slideInHorizontally(
                                animationSpec = tween(220),
                                initialOffsetX = { -it / 4 })
                        },
                        popExitTransition = {
                            if (SwipeBackNavState.suppressPopAnim) ExitTransition.None
                            else slideOutHorizontally(
                                animationSpec = tween(220),
                                targetOffsetX = { it })
                        },
                    ) {
                        NotificationListScreen(navController)
                    }
                    composable(
                        route = Screen.AiChat.ROUTE,
                        arguments = listOf(navArgument("conversationId") { type = NavType.StringType }),
                        enterTransition = {
                            slideInHorizontally(animationSpec = tween(280), initialOffsetX = { -it })
                        },
                        exitTransition = {
                            slideOutHorizontally(animationSpec = tween(280), targetOffsetX = { -it })
                        },
                        popEnterTransition = {
                            slideInHorizontally(animationSpec = tween(280), initialOffsetX = { -it })
                        },
                        popExitTransition = {
                            slideOutHorizontally(animationSpec = tween(280), targetOffsetX = { -it })
                        },
                    ) { AiChatScreen() }
                    composable(
                        route = Screen.ImChat.ROUTE,
                        arguments = listOf(navArgument("conversationId") { type = NavType.StringType }),
                        enterTransition = {
                            slideInHorizontally(animationSpec = tween(280), initialOffsetX = { it })
                        },
                        exitTransition = {
                            slideOutHorizontally(animationSpec = tween(280), targetOffsetX = { it })
                        },
                        popEnterTransition = { EnterTransition.None },
                        popExitTransition = { ExitTransition.None },
                    ) { backStackEntry ->
                        val conversationId =
                            backStackEntry.arguments?.getString("conversationId") ?: return@composable
                        ChatScreen(
                            conversationId = conversationId,
                            onBack = { navController.popBackStack() },
                            navController = navController,
                        )
                    }
                    composable(Screen.CommentPhotoPicker.route) {
                        CommentPhotoPickerScreen(navController)
                    }
                    composable(
                        route = Screen.Detail.ROUTE,
                        arguments = listOf(navArgument("noteId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        ScrimBox(backStackEntry.id) {
                            DetailScreen(
                                navController = navController,
                                noteId = backStackEntry.arguments?.getString("noteId") ?: ""
                            )
                        }
                    }
                    // 发布流程嵌套图，共享viewmodel
                    navigation(
                        startDestination = Screen.PhotoPicker.route,
                        route = Screen.Publish.route,
                    ) {
                        composable(
                            route = Screen.PhotoPicker.ROUTE,
                            arguments = listOf(
                                navArgument(Screen.PhotoPicker.ARG_REPLACE) {
                                    type = NavType.BoolType
                                    defaultValue = false
                                },
                            ),
                        ) { entry ->
                            val parentEntry = remember(entry) {
                                navController.getBackStackEntry(Screen.Publish.route)
                            }
                            val viewModel: PublishViewModel = hiltViewModel(parentEntry)
                            val videoViewModel: VideoPublishViewModel = hiltViewModel(parentEntry)
                            val replaceMode =
                                entry.arguments?.getBoolean(Screen.PhotoPicker.ARG_REPLACE) ?: false
                            PhotoPickerScreen(
                                navController = navController,
                                viewModel = viewModel,
                                videoViewModel = videoViewModel,
                                replaceMode = replaceMode,
                            )
                        }// 生命周期与整个发布流程绑定
                        composable(Screen.NotePublish.route) { entry ->
                            val parentEntry = remember(entry) {
                                navController.getBackStackEntry(Screen.Publish.route)
                            }
                            val viewModel: PublishViewModel = hiltViewModel(parentEntry)
                            PublishScreen(navController = navController, viewModel = viewModel)
                        }
                        composable(Screen.VideoPublish.route) { entry ->
                            val parentEntry = remember(entry) {
                                navController.getBackStackEntry(Screen.Publish.route)
                            }
                            val videoViewModel: VideoPublishViewModel = hiltViewModel(parentEntry)
                            VideoPublishScreen(navController = navController, viewModel = videoViewModel)
                        }
                        composable(Screen.CoverPicker.route) { entry ->
                            val parentEntry = remember(entry) {
                                navController.getBackStackEntry(Screen.Publish.route)
                            }
                            val videoViewModel: VideoPublishViewModel = hiltViewModel(parentEntry)
                            CoverPickerScreen(navController = navController, viewModel = videoViewModel)
                        }
                    }
                }
            }
        }

        // 翻译离线包管理层
        AnimatedVisibility(
            visible = showTranslatePack,
            enter = slideInHorizontally(initialOffsetX = { it }),
            exit = slideOutHorizontally(targetOffsetX = { it }),
        ) {
            TranslatePackScreen(onBack = { showTranslatePack = false })
        }
        // 草稿箱覆盖层
        AnimatedVisibility(
            visible = showDraftBox,
            enter = slideInHorizontally(initialOffsetX = { it }),
            exit = slideOutHorizontally(targetOffsetX = { it }),
        ) {
            DraftBoxOverlay(
                mainNavController = navController,
                onClose = { showDraftBox = false },
            )
        }
        AnimatedVisibility(
            visible = showAccountPrivacy,
            enter = slideInHorizontally(initialOffsetX = { it }),
            exit = slideOutHorizontally(targetOffsetX = { it }),
        ) {
            AccountPrivacyScreen(
                onClose = { showAccountPrivacy = false },
                onPasswordChanged = {
                    showAccountPrivacy = false
                    scope.launch { drawerState.close() }
                    imViewModel.logout()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onLogout = {
                    showAccountPrivacy = false
                    scope.launch { drawerState.close() }
                    authViewModel.logout()
                    imViewModel.logout()
                    messageViewModel.onLoggedOut()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
                        launchSingleTop = true
                    }
                },
            )
        }
        // 通用设置覆盖层
        AnimatedVisibility(
            visible = showGeneralSettings,
            enter = slideInHorizontally(initialOffsetX = { it }),
            exit = slideOutHorizontally(targetOffsetX = { it }),
        ) {
            GeneralSettingsScreen(onClose = { showGeneralSettings = false })
        }
        // 关于覆盖层
        AnimatedVisibility(
            visible = showAbout,
            enter = slideInHorizontally(initialOffsetX = { it }),
            exit = slideOutHorizontally(targetOffsetX = { it }),
        ) {
            AboutScreen(onClose = { showAbout = false })
        }
    }

    // 覆盖层、抽屉优先返回
    val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher// 获取分发器
    val backEnabled = (showTranslatePack || drawerState.isOpen) &&
            !showDraftBox && !showAccountPrivacy && !showGeneralSettings && !showAbout
    DisposableEffect(backDispatcher, backEnabled) {
        if (backEnabled) {
            val callback = object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (showTranslatePack) showTranslatePack = false
                    else scope.launch { drawerState.close() }
                }
            }
            backDispatcher?.addCallback(callback)
            onDispose { callback.remove() }
        } else {
            onDispose { }
        }
    }

    // 发布选择底部弹窗
    if (showPublishSheet) {
        ModalBottomSheet(
            onDismissRequest = { showPublishSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            PublishBottomSheet(
                onAlbum = {
                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                        showPublishSheet = false
                        // 新建发布前清掉可能残留的编辑标志
                        PendingNoteEdit.noteId = null
                        PendingNoteEdit.isVideo = false
                        navController.navigate(Screen.Publish.route)
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

    // 强制下线弹窗
    if (forceLogout) {
        ForceLogoutDialog(
            onConfirm = {
                forceLogout = false
                cleanupForceLogout()
                navController.navigate(Screen.Login.route) {
                    popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
                    launchSingleTop = true
                }
            },
        )
    }
}
