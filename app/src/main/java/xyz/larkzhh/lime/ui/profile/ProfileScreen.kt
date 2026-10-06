package xyz.larkzhh.lime.ui.profile

import android.app.Activity
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.yalantis.ucrop.UCrop
import java.io.File
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import xyz.larkzhh.lime.R
import xyz.larkzhh.lime.navigation.state.AuthorProfileSession
import xyz.larkzhh.lime.ui.components.LoginGate
import xyz.larkzhh.lime.navigation.state.ProfileLayoutStore
import xyz.larkzhh.lime.navigation.route.Screen
import xyz.larkzhh.lime.ui.video.feed.PersonalVideoPayload
import xyz.larkzhh.lime.ui.video.feed.VideoFeedSessionStore
import xyz.larkzhh.swipeback.SwipeBackScaffold
import xyz.larkzhh.lime.data.network.model.FeedItem
import xyz.larkzhh.lime.domain.model.FollowActionState
import xyz.larkzhh.lime.domain.model.toFollowActionState
import xyz.larkzhh.lime.ui.components.ErrorState
import xyz.larkzhh.lime.ui.components.PagingWaterfallFeed
import xyz.larkzhh.lime.ui.components.UnfollowConfirmDialog
import xyz.larkzhh.lime.ui.components.ImagePreviewOverlay
import xyz.larkzhh.lime.ui.profile.components.LikeFavStatsDialog
import xyz.larkzhh.lime.ui.profile.components.ProfileHeader
import xyz.larkzhh.lime.ui.profile.components.ProfileTabRow
import xyz.larkzhh.lime.ui.profile.components.ProfileTopBar
import xyz.larkzhh.lime.ui.profile.viewmodel.ProfileLikeState
import xyz.larkzhh.lime.ui.profile.viewmodel.ProfileNotesViewModel
import xyz.larkzhh.lime.ui.profile.viewmodel.ProfileViewModel
import xyz.larkzhh.lime.ui.im.viewmodel.ImViewModel
import xyz.larkzhh.lime.ui.theme.LimeWhite
import xyz.larkzhh.lime.ui.profile.viewmodel.ProfileUiState
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import xyz.larkzhh.lime.ui.openVideo
import xyz.larkzhh.lime.util.media.extractGradientColor
import xyz.larkzhh.lime.util.showToast

/// 主页 Tab 类型
private enum class ProfileTab(@StringRes val labelRes: Int) {
    Notes(R.string.profile_tab_notes),
    Likes(R.string.profile_tab_likes),
    Favorites(R.string.profile_tab_favorites),
}

@Composable
fun ProfileScreen(
    navController: NavHostController,
    userId: Long? = null,// null为底部导航我的
    viewModel: ProfileViewModel = hiltViewModel(),
    notesViewModel: ProfileNotesViewModel = hiltViewModel(),
    imViewModel: ImViewModel = hiltViewModel(),
    session: AuthorProfileSession? = null,// 非空为笔记作者用户页面
    onOpenDrawer: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsState()
    val isSelf by viewModel.isSelf.collectAsState()
    val imState by imViewModel.state.collectAsState()
    val user = (uiState as? ProfileUiState.Success)?.user
    val relations by viewModel.relations.collectAsState()
    val followError by viewModel.followError.collectAsState()
    val followState = user?.let { relations[it.id]?.toFollowActionState() } ?: FollowActionState.Follow
    val targetUserId = user?.id
    val uploadError by viewModel.uploadError.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val mutualFollowRequiredText = stringResource(R.string.profile_mutual_follow_required)
    var showUnfollowConfirm by remember { mutableStateOf(false) }
    var showLikeFavStats by remember { mutableStateOf(false) }
    var avatarPreviewUrl by remember { mutableStateOf<String?>(null) }// 他人头像全屏预览

    val likeState by notesViewModel.likeState.collectAsState()
    val notesPagingItems = notesViewModel.notesPager.collectAsLazyPagingItems()
    val likesPagingItems = notesViewModel.likesPager.collectAsLazyPagingItems()
    val favoritesPagingItems = notesViewModel.favoritesPager.collectAsLazyPagingItems()

    /// 本人三个tab，他人按隐私过滤
    val tabKinds = remember(user, isSelf) {
        if (isSelf) {
            listOf(ProfileTab.Notes, ProfileTab.Likes, ProfileTab.Favorites)
        } else {
            user?.let { u ->
                buildList {
                    add(ProfileTab.Notes)
                    if (!u.likePrivate) add(ProfileTab.Likes)
                    if (!u.favPrivate) add(ProfileTab.Favorites)
                }
            } ?: emptyList()
        }
    }
    val tabs = remember(tabKinds) { tabKinds.map { it.labelRes } }.map { stringResource(it) }
    // 应用会话记录
    val pagerState = rememberPagerState(initialPage = session?.currentPage ?: 0) { tabs.size }
    val coroutineScope = rememberCoroutineScope()
    if (session != null) {
        LaunchedEffect(pagerState) {
            snapshotFlow { pagerState.currentPage }.collect { session.currentPage = it }
        }
    }

    /// 接收头像裁剪结果后上传
    val avatarCropLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            UCrop.getOutput(result.data!!)?.let { viewModel.uploadAvatar(it) }
        }
    }

    /// 选图后跳转头像裁剪页
    val avatarCropTitle = stringResource(R.string.edit_avatar_crop_title)
    val avatarPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val dest = Uri.fromFile(File(context.cacheDir, "avatar_crop_tmp.jpg"))
            val intent = UCrop.of(uri, dest)
                .withOptions(UCrop.Options().apply {
                    setToolbarTitle(avatarCropTitle)
                    setCompressionQuality(90)
                    setCircleDimmedLayer(true)// 圆形遮罩
                    setToolbarColor(0xFFFFFFFF.toInt())
                    setStatusBarColor(0xFF1A1A1A.toInt())
                    setActiveControlsWidgetColor(0xFF4A9B6F.toInt())
                })
                .withAspectRatio(1f, 1f)
                .getIntent(context)
            avatarCropLauncher.launch(intent)
        }
    }

    /// 错误弹窗提示
    LaunchedEffect(uploadError) {
        uploadError?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearUploadError()
        }
    }

    LaunchedEffect(followError) {
        followError?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearFollowError()
        }
    }

    LaunchedEffect(imState.errorMessage) {
        imState.errorMessage?.let { error ->
            error.showToast(context)
            imViewModel.clearError()
        }
    }

    // 折叠 header 状态
    val layoutMetrics = remember(userId) { userId?.let { ProfileLayoutStore.getOrCreate(it) } }
    var headerHeightPx by rememberSaveable { mutableIntStateOf(layoutMetrics?.headerHeightPx ?: session?.headerHeightPx ?: 0) }
    var tabBarHeightPx by rememberSaveable { mutableIntStateOf(layoutMetrics?.tabBarHeightPx ?: session?.tabBarHeightPx ?: 0) }
    var topBarHeightPx by rememberSaveable { mutableIntStateOf(layoutMetrics?.topBarHeightPx ?: session?.topBarHeightPx ?: 0) }
    var headerOffsetPx by rememberSaveable { mutableFloatStateOf(layoutMetrics?.headerOffsetPx ?: session?.headerOffsetPx ?: 0f) }
    val density = LocalDensity.current
    val gapPxConst = with(density) { 4.dp.toPx() }
    if (layoutMetrics != null) {
        LaunchedEffect(Unit) {
            snapshotFlow { headerOffsetPx }.collect { layoutMetrics.headerOffsetPx = it }
        }
        LaunchedEffect(Unit) {
            snapshotFlow { Triple(headerHeightPx, tabBarHeightPx, topBarHeightPx) }.collect {
                layoutMetrics.headerHeightPx = it.first
                layoutMetrics.tabBarHeightPx = it.second
                layoutMetrics.topBarHeightPx = it.third
            }
        }
    }
    if (session != null) {
        LaunchedEffect(Unit) {
            snapshotFlow { headerOffsetPx }.collect { session.headerOffsetPx = it }
        }
        LaunchedEffect(Unit) {
            snapshotFlow { Triple(headerHeightPx, tabBarHeightPx, topBarHeightPx) }.collect {
                session.headerHeightPx = it.first
                session.tabBarHeightPx = it.second
                session.topBarHeightPx = it.third
            }
        }
    }

    // 恢复会话记录的tab滚动位置
    val notesScrollState = rememberLazyStaggeredGridState(
        initialFirstVisibleItemIndex = session?.tabScroll?.get(ProfileTab.Notes.ordinal)?.first ?: 0,
        initialFirstVisibleItemScrollOffset = session?.tabScroll?.get(ProfileTab.Notes.ordinal)?.second ?: 0,
    )
    val likesScrollState = rememberLazyStaggeredGridState(
        initialFirstVisibleItemIndex = session?.tabScroll?.get(ProfileTab.Likes.ordinal)?.first ?: 0,
        initialFirstVisibleItemScrollOffset = session?.tabScroll?.get(ProfileTab.Likes.ordinal)?.second ?: 0,
    )
    val favoritesScrollState = rememberLazyStaggeredGridState(
        initialFirstVisibleItemIndex = session?.tabScroll?.get(ProfileTab.Favorites.ordinal)?.first ?: 0,
        initialFirstVisibleItemScrollOffset = session?.tabScroll?.get(ProfileTab.Favorites.ordinal)?.second ?: 0,
    )
    if (session != null) {
        LaunchedEffect(notesScrollState) {
            snapshotFlow { notesScrollState.firstVisibleItemIndex to notesScrollState.firstVisibleItemScrollOffset }
                .collect { session.tabScroll[ProfileTab.Notes.ordinal] = it }
        }
        LaunchedEffect(likesScrollState) {
            snapshotFlow { likesScrollState.firstVisibleItemIndex to likesScrollState.firstVisibleItemScrollOffset }
                .collect { session.tabScroll[ProfileTab.Likes.ordinal] = it }
        }
        LaunchedEffect(favoritesScrollState) {
            snapshotFlow { favoritesScrollState.firstVisibleItemIndex to favoritesScrollState.firstVisibleItemScrollOffset }
                .collect { session.tabScroll[ProfileTab.Favorites.ordinal] = it }
        }
    }

    // 背景图主色提取
    val backgroundUrl = (uiState as? ProfileUiState.Success)?.user?.backgroundImage
    // 主色存进会话
    var dominantColor by remember {
        mutableStateOf(session?.backgroundDominantRgb?.let { Color(it) } ?: Color.Black)
    }
    LaunchedEffect(backgroundUrl) {
        val source = backgroundUrl
            ?: "android.resource://${context.packageName}/${R.drawable.bg}"
        val rgb = extractGradientColor(context, source)
        dominantColor = if (rgb != null) Color(rgb) else Color.Black
        if (rgb != null && backgroundUrl != null) session?.backgroundDominantRgb = rgb
    }
    val gradientEndColor = dominantColor.copy(alpha = 0.95f)

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            //计算header最多能向上隐藏的像素
            private fun minOffset() =
                -(headerHeightPx.toFloat() - topBarHeightPx.toFloat() - gapPxConst).coerceAtLeast(0f)

            // 上滑header先折叠，再交给list
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y >= 0f) return Offset.Zero
                val old = headerOffsetPx
                headerOffsetPx = (old + available.y).coerceIn(minOffset(), 0f)
                return Offset(0f, headerOffsetPx - old)
            }

            // 下滑list滚到顶后剩余才展开header
            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (available.y <= 0f) return Offset.Zero
                val old = headerOffsetPx
                headerOffsetPx = (old + available.y).coerceIn(minOffset(), 0f)
                return Offset(0f, headerOffsetPx - old)
            }
        }
    }

    // 右滑手势分区
    var tabContentTopPx by remember { mutableFloatStateOf(Float.MAX_VALUE) }// tab区域

    SwipeBackScaffold(
        backEnabled = userId != null,
        tabContentRegion = if (userId != null) {
            { pos -> pos.y >= tabContentTopPx }
        } else null,
        tabAtLeftmost = { pagerState.currentPage == 0 && pagerState.currentPageOffsetFraction >= 0f },
    ) {
    val fromBottomNav = userId == null
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets(0),
    ) { padding ->
        val currentTab = tabKinds.getOrNull(pagerState.currentPage) ?: ProfileTab.Notes
        val currentIsRefreshing = when (currentTab) {
            ProfileTab.Notes -> notesPagingItems.loadState.refresh is LoadState.Loading
            ProfileTab.Likes -> likesPagingItems.loadState.refresh is LoadState.Loading
            ProfileTab.Favorites -> favoritesPagingItems.loadState.refresh is LoadState.Loading
        }
        // 根据tab页选择刷新方法
        val onRefresh: () -> Unit = when (currentTab) {
            ProfileTab.Notes -> notesPagingItems::refresh
            ProfileTab.Likes -> likesPagingItems::refresh
            ProfileTab.Favorites -> favoritesPagingItems::refresh
        }
        val refreshState = rememberPullToRefreshState()
        PullToRefreshBox(
            isRefreshing = currentIsRefreshing,
            onRefresh = onRefresh,
            state = refreshState,
            modifier = Modifier.fillMaxSize(),
            indicator = {
                PullToRefreshDefaults.Indicator(
                    state = refreshState,
                    isRefreshing = currentIsRefreshing,
                    containerColor = MaterialTheme.colorScheme.surface,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.align(Alignment.TopCenter),
                )
            },
        ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .nestedScroll(nestedScrollConnection)
        ) {
            val overlapPx = with(density) { 24.dp.toPx() }
            val gapPx = with(density) { 4.dp.toPx() }
            val visibleHeaderPx = (headerHeightPx + headerOffsetPx).coerceAtLeast(0f)// header 当前实际可见的高度
            val tabBarTopPx = (visibleHeaderPx - overlapPx).coerceAtLeast(topBarHeightPx.toFloat() + gapPx)// tab 栏距离顶部的距离
            SideEffect { tabContentTopPx = tabBarTopPx + tabBarHeightPx }// tab 内容区顶部 = tab 栏底部
            val contentTopDp: Dp = with(density) { (tabBarTopPx + tabBarHeightPx).toDp() }// 列表内容起始位置
            val stickyTabBarBottomDp: Dp = with(density) {
                (topBarHeightPx.toFloat() + gapPx + tabBarHeightPx.toFloat()).toDp()
            }// tab 栏吸顶时的绝对位置
            val relativeContentPaddingTop: Dp = (contentTopDp - stickyTabBarBottomDp).coerceAtLeast(8.dp)// 列表内容顶部内边距
            val maxScrollPx = (headerHeightPx.toFloat() - topBarHeightPx.toFloat() - gapPx).coerceAtLeast(1f)// header 实际可滚动距离
            val scrollFraction = if (headerHeightPx > 0) (-headerOffsetPx / maxScrollPx).coerceIn(0f, 1f) else 0f
            val topBarBgAlpha = ((scrollFraction - 0.2f) / 0.5f).coerceIn(0f, 1f)// 顶部栏背景透明度
            val editButtonAlpha = (1f - scrollFraction / 0.5f).coerceIn(0f, 1f)// 编辑按钮背景透明度
            val avatarThresholdPx = with(density) { 84.dp.toPx() }
            val miniAvatarProgress = ((-headerOffsetPx - avatarThresholdPx) / with(density) { 32.dp.toPx() }).coerceIn(0f, 1f)// 小头像的过渡进度
            val miniAvatarAlpha = miniAvatarProgress// 小头像透明度
            val miniAvatarOffsetDp: Dp = with(density) { ((1f - miniAvatarAlpha) * 12.dp.toPx()).toDp() }// 小头像偏移

            if (tabKinds.isNotEmpty()) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.padding(top = stickyTabBarBottomDp).fillMaxSize().clip(RectangleShape),
                    beyondViewportPageCount = 1,
                ) { page ->
                    when (tabKinds.getOrNull(page)) {
                        ProfileTab.Notes -> TabPage(
                            pagingItems = notesPagingItems,
                            likeState = likeState,
                            contentPaddingTop = relativeContentPaddingTop,
                            navController = navController,
                            onLikeToggle = notesViewModel::toggleLike,
                            state = notesScrollState,
                        )
                        ProfileTab.Likes -> TabPage(
                            pagingItems = likesPagingItems,
                            likeState = likeState,
                            contentPaddingTop = relativeContentPaddingTop,
                            navController = navController,
                            onLikeToggle = notesViewModel::toggleLike,
                            state = likesScrollState,
                        )
                        ProfileTab.Favorites -> TabPage(
                            pagingItems = favoritesPagingItems,
                            likeState = likeState,
                            contentPaddingTop = relativeContentPaddingTop,
                            navController = navController,
                            onLikeToggle = notesViewModel::toggleLike,
                            state = favoritesScrollState,
                        )
                        null -> Unit
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = with(density) { headerHeightPx.toDp() }),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(28.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = LimeWhite,
                        strokeWidth = 2.dp,
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
                isSelf = isSelf,
                gradientEndColor = gradientEndColor,
                onEditAvatar = { avatarPickerLauncher.launch("image/*") },
                onAvatarClick = { user?.avatar?.let { avatarPreviewUrl = it } },
                onBrowseHistory = { navController.navigate(Screen.BrowseHistory.route) },
                onGroupChat = { navController.navigate(Screen.GroupList.route) },
                onFollowClick = {
                    // 关注他人登录拦截
                    if (!LoginGate.onRequireLogin(null)) {
                        if (followState == FollowActionState.Follow) {
                            viewModel.follow()
                        } else {
                            showUnfollowConfirm = true
                        }
                    }
                },
                onMessageClick = {
                    // 私信录拦截
                    if (!LoginGate.onRequireLogin(null)) {
                        targetUserId?.let { target ->
                            if (followState != FollowActionState.Mutual) {
                                mutualFollowRequiredText.showToast(context)
                            } else {
                                imViewModel.openConversation(target) { conversationId ->
                                    navController.navigate(Screen.ImChat.createRoute(conversationId)) {
                                        launchSingleTop = true
                                    }
                                }
                            }
                        }
                    }
                },
                onFollowingClick = {
                    targetUserId?.let { target ->
                        // 关注、粉丝列表登录拦截
                        val route = Screen.FollowList.createRoute(target, Screen.FollowList.TAB_FOLLOWING)
                        if (!LoginGate.onRequireLogin(route)) {
                            navController.navigate(route)
                        }
                    }
                },
                onFollowersClick = {
                    targetUserId?.let { target ->
                        val route = Screen.FollowList.createRoute(target, Screen.FollowList.TAB_FOLLOWERS)
                        if (!LoginGate.onRequireLogin(route)) {
                            navController.navigate(route)
                        }
                    }
                },
                onLikeFavClick = { showLikeFavStats = true },
                followState = followState,
            )

            // Tab 栏
            if (tabKinds.isNotEmpty()) {
                val isSticky = (visibleHeaderPx - overlapPx) <= topBarHeightPx.toFloat() + gapPx// 是否吸顶
                val cornerRadiusDp by animateDpAsState(
                    targetValue = if (isSticky) 0.dp else 16.dp,
                    label = "tabBarCorner"
                )
                val tabShape = RoundedCornerShape(topStart = cornerRadiusDp, topEnd = cornerRadiusDp)
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

            // 顶部栏
            ProfileTopBar(
                user = user,
                bgAlpha = topBarBgAlpha,
                dominantColor = dominantColor,
                miniAvatarAlpha = miniAvatarAlpha,
                miniAvatarOffsetDp = miniAvatarOffsetDp,
                editButtonAlpha = editButtonAlpha,
                leadingIcon = if (fromBottomNav) Icons.Default.Menu else Icons.AutoMirrored.Filled.ArrowBack,
                onLeadingClick = {
                    if (fromBottomNav) {
                        onOpenDrawer()
                    } else {
                        navController.popBackStack()
                    }
                },
                showTrailingActions = isSelf,
                onEditProfileClick = { navController.navigate(Screen.EditProfile.route) },
                onQrScanClick = { navController.navigate(Screen.QrScan.route) },
                onSizeChanged = { size -> topBarHeightPx = size.height },
            )
        }
        }
    }

    if (showUnfollowConfirm) {
        UnfollowConfirmDialog(
            onCancel = { showUnfollowConfirm = false },
            onConfirm = {
                viewModel.unfollow()
                showUnfollowConfirm = false
            },
        )
    }

    // 获赞与收藏统计弹窗
    if (showLikeFavStats) {
        if (user != null) {
            LikeFavStatsDialog(
                noteCount = user.noteCount ?: 0,
                likeCount = user.totalLikeCount ?: 0,
                favCount = user.totalFavCount ?: 0,
                onDismiss = { showLikeFavStats = false },
            )
        }
    }

    // 他人头像全屏预览
    val avatarPreview = avatarPreviewUrl
    if (avatarPreview != null) {
        Dialog(
            onDismissRequest = { avatarPreviewUrl = null },
            properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
        ) {
            ImagePreviewOverlay(
                images = listOf(avatarPreview),
                initialIndex = 0,
                onDismiss = { avatarPreviewUrl = null },
            )
        }
    }
    }
}

/// Tab 页面
@Composable
private fun TabPage(
    pagingItems: LazyPagingItems<FeedItem>,
    likeState: ProfileLikeState,
    contentPaddingTop: Dp,
    navController: NavHostController,
    onLikeToggle: (FeedItem, Boolean, Int) -> Unit,
    state: LazyStaggeredGridState = rememberLazyStaggeredGridState(),
) {
    val context = LocalContext.current
    val refreshState = pagingItems.loadState.refresh
    val stateModifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant)

    when (refreshState) {
        // 无缓存首屏加载
        is LoadState.Loading if pagingItems.itemCount == 0 -> {
            Box(
                modifier = stateModifier.padding(top = contentPaddingTop),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(28.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = LimeWhite,
                    strokeWidth = 2.dp,
                )
            }
        }
        // 无缓存且加载失败
        is LoadState.Error if pagingItems.itemCount == 0 -> {
            Box(
                modifier = stateModifier.padding(top = contentPaddingTop),
                contentAlignment = Alignment.Center,
            ) {
                ErrorState(
                    message = refreshState.error.message,
                    onRetry = { pagingItems.retry() },
                )
            }
        }
        // 有内容
        else -> {
            PagingWaterfallFeed(
                pagingItems = pagingItems,
                likeStates = likeState.likeStates,
                likeCounts = likeState.likeCounts,
                onLikeToggle = onLikeToggle,
                onItemClick = { item ->
                    // 视频笔记进竖屏视频页，图文进详情
                    if (item.noteType == 2) {
                        val snapshot = pagingItems.itemSnapshotList.items
                        VideoFeedSessionStore.put(
                            item.id,
                            PersonalVideoPayload(
                                items = snapshot,
                                startIndex = snapshot.indexOfFirst { it.id == item.id }.coerceAtLeast(0),
                            ),
                        )
                        if (navController.graph.findNode(Screen.VideoFeed.ROUTE) != null) {
                            navController.navigate(
                                Screen.VideoFeed.createRoute(item.id, Screen.VideoFeed.SOURCE_PERSONAL),
                            )
                        } else {
                            context.openVideo(item.id, Screen.VideoFeed.SOURCE_PERSONAL)
                        }
                    } else {
                        navController.navigate(
                            Screen.Detail.createRoute(item.id.toString())
                        )
                    }
                },
                state = state,
                contentPadding = PaddingValues(start = 5.dp, end = 5.dp, top = contentPaddingTop, bottom = 8.dp),
            )
        }
    }
}
