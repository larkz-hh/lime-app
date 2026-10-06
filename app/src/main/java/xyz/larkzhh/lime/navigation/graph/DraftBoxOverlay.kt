package xyz.larkzhh.lime.navigation.graph

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import xyz.larkzhh.lime.navigation.route.Screen
import xyz.larkzhh.lime.ui.draft.DraftBoxScreen
import xyz.larkzhh.lime.ui.draft.DraftListRefresh
import xyz.larkzhh.lime.ui.comment.CommentPhotoPickerScreen
import xyz.larkzhh.lime.ui.publish.CoverPickerScreen
import xyz.larkzhh.lime.ui.publish.PhotoPickerScreen
import xyz.larkzhh.lime.ui.publish.PublishScreen
import xyz.larkzhh.lime.ui.publish.VideoPublishScreen
import xyz.larkzhh.lime.ui.publish.viewmodel.PublishViewModel
import xyz.larkzhh.lime.ui.publish.viewmodel.VideoPublishViewModel

private const val DRAFT_LIST_ROUTE = "draft_list"

/**
 * 草稿箱覆盖层。
 */
@Composable
fun DraftBoxOverlay(
    mainNavController: NavHostController,
    onClose: () -> Unit,
) {
    val innerNavController = rememberNavController()

    BackHandler {
        if (!innerNavController.popBackStack()) onClose()
    }

    NavHost(
        navController = innerNavController,
        startDestination = DRAFT_LIST_ROUTE,
    ) {
        // 草稿列表
        composable(DRAFT_LIST_ROUTE) {
            DraftBoxScreen(navController = innerNavController, onClose = onClose)
        }
        composable(Screen.Home.route) {
            LaunchedEffect(Unit) {
                onClose()// 关闭草稿箱覆盖层
                mainNavController.navigate(Screen.Home.route) {
                    popUpTo(Screen.Home.route) { inclusive = false }// 主导航回首页
                }
            }
            Box(modifier = Modifier.fillMaxSize())
        }
        // 发布流程
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
                    innerNavController.getBackStackEntry(Screen.Publish.route)
                }
                val viewModel: PublishViewModel = hiltViewModel(parentEntry)
                val videoViewModel: VideoPublishViewModel = hiltViewModel(parentEntry)
                val replaceMode =
                    entry.arguments?.getBoolean(Screen.PhotoPicker.ARG_REPLACE) ?: false
                PhotoPickerScreen(
                    navController = innerNavController,
                    viewModel = viewModel,
                    videoViewModel = videoViewModel,
                    replaceMode = replaceMode,
                )
            }
            composable(Screen.NotePublish.route) { entry ->
                val parentEntry = remember(entry) {
                    innerNavController.getBackStackEntry(Screen.Publish.route)
                }
                val viewModel: PublishViewModel = hiltViewModel(parentEntry)
                PublishScreen(
                    navController = innerNavController,
                    viewModel = viewModel,
                    onDraftSaved = {
                        // 存草稿后留在草稿箱，弹回列表并刷新
                        DraftListRefresh.pending = true
                        if (!innerNavController.popBackStack(Screen.Publish.route, inclusive = true)) {
                            innerNavController.popBackStack()
                        }
                    },
                )
            }
            composable(Screen.VideoPublish.route) { entry ->
                val parentEntry = remember(entry) {
                    innerNavController.getBackStackEntry(Screen.Publish.route)
                }
                val videoViewModel: VideoPublishViewModel = hiltViewModel(parentEntry)
                VideoPublishScreen(
                    navController = innerNavController,
                    viewModel = videoViewModel,
                    onDraftSaved = {
                        DraftListRefresh.pending = true
                        if (!innerNavController.popBackStack(Screen.Publish.route, inclusive = true)) {
                            innerNavController.popBackStack()
                        }
                    },
                )
            }
            composable(Screen.CoverPicker.route) { entry ->
                val parentEntry = remember(entry) {
                    innerNavController.getBackStackEntry(Screen.Publish.route)
                }
                val videoViewModel: VideoPublishViewModel = hiltViewModel(parentEntry)
                CoverPickerScreen(navController = innerNavController, viewModel = videoViewModel)
            }
        }
        // 图文草稿相册选择页
        composable(Screen.CommentPhotoPicker.route) { CommentPhotoPickerScreen(innerNavController) }
    }
}
