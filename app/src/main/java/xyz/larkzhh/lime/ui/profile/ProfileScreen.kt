package xyz.larkzhh.lime.ui.profile

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import xyz.larkzhh.lime.navigation.Screen
import xyz.larkzhh.lime.ui.components.NoteCard
import xyz.larkzhh.lime.ui.components.WaterfallFeed
import xyz.larkzhh.lime.ui.profile.components.ProfileHeader
import xyz.larkzhh.lime.ui.profile.components.ProfileTabRow
import xyz.larkzhh.lime.ui.profile.viewmodel.ProfileNotesViewModel
import xyz.larkzhh.lime.ui.profile.viewmodel.ProfileViewModel
import xyz.larkzhh.lime.ui.theme.LimeGray
import xyz.larkzhh.lime.ui.theme.LimeLightGray
import xyz.larkzhh.lime.ui.theme.LimePrimary
import xyz.larkzhh.lime.ui.theme.LimeWhite

@Composable
fun ProfileScreen(
    navController: NavHostController,
    viewModel: ProfileViewModel = hiltViewModel(),
    notesViewModel: ProfileNotesViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val uploadError by viewModel.uploadError.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    val notesUiState by notesViewModel.uiState.collectAsState()
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }// tab
    val tabs = listOf("笔记", "点赞", "收藏")

    /// 选择图片上传头像
    val avatarPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> if (uri != null) viewModel.uploadAvatar(uri) }

    /// 错误弹窗提示
    LaunchedEffect(uploadError) {
        uploadError?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearUploadError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = LimeLightGray,
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            WaterfallFeed(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface),
                isLoadingMore = notesUiState.isLoadingMore,
                onLoadMore = notesViewModel::loadMore,
                contentPadding = PaddingValues(bottom = innerPadding.calculateBottomPadding()),
                verticalItemSpacing = 0.dp,
            ) {
                // 头部区域
                item(span = StaggeredGridItemSpan.FullLine) {
                    ProfileHeader(
                        uiState = uiState,
                        onEditProfile = { navController.navigate("edit_profile") },
                        onEditAvatar = { avatarPickerLauncher.launch("image/*") },
                    )
                }

                // tab 栏
                item(span = StaggeredGridItemSpan.FullLine) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .layout { measurable, constraints ->
                                val placeable = measurable.measure(constraints)
                                val overlapPx = 12.dp.roundToPx()
                                layout(placeable.width, (placeable.height - overlapPx).coerceAtLeast(0)) {
                                    placeable.placeRelative(0, -overlapPx)
                                }
                            }
                            .shadow(
                                elevation = 4.dp,
                                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                                clip = false,
                            )
                            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                            .background(MaterialTheme.colorScheme.surface)
                    ) {
                        ProfileTabRow(
                            tabs = tabs,
                            selectedIndex = selectedTab,
                            onTabSelected = { selectedTab = it },
                        )
                        HorizontalDivider(
                            thickness = 0.5.dp,
                            color = LimeLightGray,
                        )
                    }
                }

                // tab 内容
                item(span = StaggeredGridItemSpan.FullLine) {
                    Box(modifier = Modifier.fillMaxWidth().height(8.dp))
                }
                when (selectedTab) {
                    0 -> {
                        // 笔记列表
                        if (notesUiState.isLoading) {
                            item(span = StaggeredGridItemSpan.FullLine) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 32.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(28.dp),
                                        color = LimePrimary,
                                        trackColor = LimeWhite,
                                        strokeWidth = 2.dp,
                                    )
                                }
                            }
                        } else if (notesUiState.error != null && notesUiState.items.isEmpty()) {
                            item(span = StaggeredGridItemSpan.FullLine) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 32.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = notesUiState.error ?: "加载失败",
                                        color = LimeGray,
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                }
                            }
                        } else {
                            items(notesUiState.items, key = { it.id }) { item ->
                                NoteCard(
                                    item = item,
                                    liked = item.id in notesUiState.likedIds,
                                    onLikeToggle = { notesViewModel.toggleLike(item.id) },
                                    modifier = Modifier.padding(
                                        start = 4.dp,
                                        end = 4.dp,
                                        bottom = 8.dp,
                                    ),
                                    onClick = {
                                        navController.navigate(
                                            Screen.Detail.createRoute(item.id.toString())
                                        )
                                    },
                                )
                            }
                        }
                    }
                    else -> {
                        item(span = StaggeredGridItemSpan.FullLine) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 64.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = "依旧施工",
                                    color = LimeGray,
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
