package xyz.larkzhh.lime.navigation.graph
import xyz.larkzhh.swipeback.SwipeBackNavState
import xyz.larkzhh.swipeback.ScrimBox
import xyz.larkzhh.lime.navigation.route.Screen
import xyz.larkzhh.lime.ui.profile.state.AuthorProfileStore

import androidx.activity.compose.BackHandler
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import xyz.larkzhh.lime.ui.detail.DetailScreen
import xyz.larkzhh.lime.ui.detail.comment.CommentPhotoPickerScreen
import xyz.larkzhh.lime.ui.profile.ProfileScreen
import xyz.larkzhh.lime.ui.publish.CoverPickerScreen
import xyz.larkzhh.lime.ui.publish.PhotoPickerScreen
import xyz.larkzhh.lime.ui.publish.PublishScreen
import xyz.larkzhh.lime.ui.publish.VideoPublishScreen
import xyz.larkzhh.lime.ui.publish.viewmodel.PublishViewModel
import xyz.larkzhh.lime.ui.publish.viewmodel.VideoPublishViewModel
import xyz.larkzhh.lime.ui.profile.edit.EditProfileScreen
import xyz.larkzhh.lime.ui.profile.history.BrowseHistoryScreen
import xyz.larkzhh.lime.ui.qrscan.QrScanScreen
import xyz.larkzhh.lime.ui.video.feed.VideoFeedScreen
import xyz.larkzhh.lime.ui.video.feed.VideoFeedViewModel
import xyz.larkzhh.lime.ui.video.player.SyncPiPPlayState
import xyz.larkzhh.lime.ui.video.player.VideoPage
import xyz.larkzhh.lime.ui.video.player.VideoPlayerManager

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun VideoNavGraph(
    noteId: Long,
    source: String,
    isInPip: Boolean,
    playerManager: VideoPlayerManager,
    onEnterMiniPlayer: (videoWidth: Int, videoHeight: Int) -> Unit,
    onPipPausedChanged: (paused: Boolean) -> Unit = {},
    onExit: () -> Unit,
) {
    val navController = rememberNavController()

    BackHandler {
        if (!navController.popBackStack()) onExit()
    }

    NavHost(
        navController = navController,
        startDestination = Screen.VideoFeed.createRoute(noteId, source),
    ) {
        composable(
            route = Screen.VideoFeed.ROUTE,
            arguments = listOf(
                navArgument("noteId") { type = NavType.LongType },
                navArgument("source") {
                    type = NavType.StringType
                    defaultValue = Screen.VideoFeed.SOURCE_RECOMMENDATION
                },
            ),
        ) {
            if (isInPip) {
                val viewModel: VideoFeedViewModel = hiltViewModel()
                val uiState by viewModel.uiState.collectAsState()
                val item = uiState.items.getOrNull(uiState.currentIndex)
                if (item != null) {
                    // 小窗与 app 内视频状态同步
                    SyncPiPPlayState(
                        noteId = item.id,
                        playerManager = playerManager,
                        onPausedChanged = { paused ->
                            viewModel.setPaused(item.id, paused)
                            onPipPausedChanged(paused)
                        },
                    )
                    VideoPage(
                        noteId = item.id,
                        playUrl = item.video.playUrl,
                        width = item.video.width,
                        height = item.video.height,
                        isActive = true,
                        userPaused = item.id in uiState.pausedNoteIds,
                        playerManager = playerManager,
                        onTogglePlay = {},
                        title = item.title,
                        showPauseIcon = false,
                    )
                }
            } else {
                VideoFeedScreen(
                    navController = navController,
                    playerManager = playerManager,
                    onEnterMiniPlayer = onEnterMiniPlayer,
                    onExit = onExit,
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
                    slideInHorizontally(initialOffsetX = { it })
                }
            },
            popEnterTransition = {
                when {
                    SwipeBackNavState.suppressPopAnim -> EnterTransition.None
                    else -> slideInHorizontally(animationSpec = tween(220), initialOffsetX = { -it / 4 })
                }
            },
            popExitTransition = {
                when {
                    SwipeBackNavState.suppressPopAnim -> ExitTransition.None
                    else -> slideOutHorizontally(animationSpec = tween(220), targetOffsetX = { it })
                }
            },
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getLong("userId") ?: return@composable
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
            route = Screen.Detail.ROUTE,
            arguments = listOf(navArgument("noteId") { type = NavType.StringType }),
        ) { backStackEntry ->
            ScrimBox(backStackEntry.id) {
                DetailScreen(
                    navController = navController,
                    noteId = backStackEntry.arguments?.getString("noteId") ?: ""
                )
            }
        }
        composable(Screen.BrowseHistory.route) { BrowseHistoryScreen(navController) }
        composable(Screen.EditProfile.route) { EditProfileScreen(navController) }
        composable(Screen.QrScan.route) { QrScanScreen(navController) }
        composable(Screen.CommentPhotoPicker.route) { CommentPhotoPickerScreen(navController) }
        // 发布、编辑流程嵌套图
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
            }
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
