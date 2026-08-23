package xyz.larkzhh.lime.navigation

import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import xyz.larkzhh.lime.ui.detail.DetailScreen
import xyz.larkzhh.lime.ui.detail.comment.CommentPhotoPickerScreen
import xyz.larkzhh.lime.ui.profile.ProfileScreen
import xyz.larkzhh.lime.ui.profile.edit.EditProfileScreen
import xyz.larkzhh.lime.ui.profile.history.BrowseHistoryScreen
import xyz.larkzhh.lime.ui.qrscan.QrScanScreen
import xyz.larkzhh.lime.ui.video.feed.VideoFeedScreen
import xyz.larkzhh.lime.ui.video.feed.VideoFeedViewModel
import xyz.larkzhh.lime.ui.video.player.SyncPiPPlayState
import xyz.larkzhh.lime.ui.video.player.VideoPage
import xyz.larkzhh.lime.ui.video.player.VideoPlayerManager

@OptIn(UnstableApi::class)
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
    }
}
