package xyz.larkzhh.lime.ui.profile

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridScope
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import xyz.larkzhh.lime.navigation.Screen
import xyz.larkzhh.lime.ui.components.NoteCard
import xyz.larkzhh.lime.ui.components.WaterfallFeed
import xyz.larkzhh.lime.ui.profile.components.ProfileHeader
import xyz.larkzhh.lime.ui.profile.components.ProfileTabRow
import xyz.larkzhh.lime.ui.profile.viewmodel.ProfileNotesUiState
import xyz.larkzhh.lime.ui.profile.viewmodel.ProfileNotesViewModel
import xyz.larkzhh.lime.ui.profile.viewmodel.ProfileViewModel
import xyz.larkzhh.lime.ui.theme.LimeGray
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

    val notesUiState by notesViewModel.notesState.collectAsState()
    val likesUiState by notesViewModel.likesState.collectAsState()
    val favoritesUiState by notesViewModel.favoritesState.collectAsState()

    val tabs = listOf("笔记", "点赞", "收藏")
    val pagerState = rememberPagerState { tabs.size }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            when (page) {
                1 -> notesViewModel.loadLikesLazy()
                2 -> notesViewModel.loadFavoritesLazy()
            }
        }
    }

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

    // 折叠 header 状态
    var headerHeightPx by remember { mutableIntStateOf(0) }
    var tabBarHeightPx by remember { mutableIntStateOf(0) }
    var headerOffsetPx by remember { mutableFloatStateOf(0f) }
    val density = LocalDensity.current

    // 向上滑先折叠 header
    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val delta = available.y// 手指滑动y轴距离
                val oldOffset = headerOffsetPx
                headerOffsetPx = (oldOffset + delta).coerceIn(-headerHeightPx.toFloat(), 0f)
                return Offset(0f, headerOffsetPx - oldOffset)// 拦截头部区域实际移动的距离
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets(0),
    ) { _ ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(nestedScrollConnection)
        ) {
            val overlapPx = with(density) { 24.dp.toPx() }
            val visibleHeaderPx = (headerHeightPx + headerOffsetPx).coerceAtLeast(0f)// header 当前实际可见的高度
            val tabBarTopPx = (visibleHeaderPx - overlapPx).coerceAtLeast(0f)// tab 栏距离顶部的距离
            val contentTopDp: Dp = with(density) { (tabBarTopPx + tabBarHeightPx).toDp() }// 列表内容开始的位置

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                beyondViewportPageCount = 1,
            ) { page ->
                when (page) {
                    0 -> TabPage(
                        uiState = notesUiState,
                        contentPaddingTop = contentTopDp,// 随 header 折叠动态变化
                        navController = navController,
                        onLikeToggle = notesViewModel::toggleLike,
                        onLoadMore = notesViewModel::loadMoreNotes,
                    )
                    1 -> TabPage(
                        uiState = likesUiState,
                        contentPaddingTop = contentTopDp,
                        navController = navController,
                        onLikeToggle = notesViewModel::toggleLike,
                        onLoadMore = notesViewModel::loadMoreLikes,
                    )
                    else -> TabPage(
                        uiState = favoritesUiState,
                        contentPaddingTop = contentTopDp,
                        navController = navController,
                        onLikeToggle = notesViewModel::toggleLike,
                        onLoadMore = notesViewModel::loadMoreFavorites,
                    )
                }
            }

            // 头部区域
            ProfileHeader(
                modifier = Modifier
                    .fillMaxWidth()
                    .onSizeChanged { headerHeightPx = it.height }
                    .offset { IntOffset(0, headerOffsetPx.roundToInt()) },
                uiState = uiState,
                onEditProfile = { navController.navigate("edit_profile") },
                onEditAvatar = { avatarPickerLauncher.launch("image/*") },
                onQrScan = { navController.navigate(Screen.QrScan.route) },
                onBrowseHistory = { navController.navigate(Screen.BrowseHistory.route) },
            )

            // Tab 栏
            val isSticky = visibleHeaderPx <= overlapPx// 是否吸顶
            val tabShape = if (isSticky) RectangleShape else RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .onSizeChanged { tabBarHeightPx = it.height }
                    .offset { IntOffset(0, tabBarTopPx.roundToInt()) }
                    .then(
                        if (!isSticky) Modifier.shadow(elevation = 4.dp, shape = tabShape, clip = false)
                        else Modifier
                    )
                    .clip(tabShape)
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                ProfileTabRow(
                    tabs = tabs,
                    selectedIndex = pagerState.currentPage,
                    onTabSelected = { index ->
                        coroutineScope.launch { pagerState.animateScrollToPage(index) }
                    },
                )
            }
        }
    }
}

/// Tab 页面
@Composable
private fun TabPage(
    uiState: ProfileNotesUiState,
    contentPaddingTop: Dp,
    navController: NavHostController,
    onLikeToggle: (Long) -> Unit,
    onLoadMore: () -> Unit,
) {
    WaterfallFeed(
        modifier = Modifier.fillMaxSize(),
        isLoadingMore = uiState.isLoadingMore,
        onLoadMore = onLoadMore,
        contentPadding = PaddingValues(top = contentPaddingTop, bottom = 8.dp),
        verticalItemSpacing = 0.dp,
    ) {
        tabContent(
            uiState = uiState,
            navController = navController,
            onLikeToggle = onLikeToggle,
        )
    }
}

/// 列表渲染逻辑
private fun LazyStaggeredGridScope.tabContent(
    uiState: ProfileNotesUiState,
    navController: NavHostController,
    onLikeToggle: (Long) -> Unit,
) {
    when {
        uiState.isLoading -> {
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
        }
        uiState.error != null && uiState.items.isEmpty() -> {
            item(span = StaggeredGridItemSpan.FullLine) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = uiState.error,
                        color = LimeGray,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
        else -> {
            items(uiState.items, key = { it.id }) { item ->
                NoteCard(
                    item = item,
                    liked = item.id in uiState.likedIds,
                    onLikeToggle = { onLikeToggle(item.id) },
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
}
