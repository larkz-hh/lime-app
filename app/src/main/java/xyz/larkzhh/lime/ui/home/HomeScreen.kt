package xyz.larkzhh.lime.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.paging.LoadState
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalContext
import xyz.larkzhh.lime.R
import xyz.larkzhh.lime.data.network.model.FeedItem
import xyz.larkzhh.lime.navigation.Screen
import xyz.larkzhh.lime.openVideo
import xyz.larkzhh.lime.ui.components.ErrorState
import xyz.larkzhh.lime.ui.components.FeedSkeleton
import xyz.larkzhh.lime.ui.components.PagingWaterfallFeed
import xyz.larkzhh.lime.ui.theme.LimeGray
import xyz.larkzhh.lime.ui.theme.LimeLightGray
import xyz.larkzhh.lime.ui.theme.LimePrimary
import xyz.larkzhh.lime.ui.theme.LimeWhite

private const val PRELOAD_COUNT = 4


private const val TAB_FOLLOW = 0// 关注
private const val TAB_DISCOVER = 1// 发现
/// 进入首页默认选中
private const val DEFAULT_TAB_INDEX = TAB_DISCOVER

@Composable
fun HomeScreen(navController: NavHostController) {
    val tabs = listOf("关注", "发现")
    val pagerState = rememberPagerState(
        initialPage = DEFAULT_TAB_INDEX,
        pageCount = { tabs.size },
    )
    val coroutineScope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize()) {
        HomeTopBar(
            tabs = tabs,
            selectedIndex = pagerState.currentPage,
            onChatClick = {
                navController.navigate(Screen.AiChat.createRoute(Screen.AiChat.LATEST_CONVERSATION))
            },
            onSearchClick = { navController.navigate(Screen.Search.BASE_ROUTE) },
            onTabSelected = { index ->
                coroutineScope.launch { pagerState.animateScrollToPage(index) }
            },
        )
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
        ) { page ->
            when (page) {
                TAB_FOLLOW -> FollowTab(navController)
                TAB_DISCOVER -> DiscoverTab(navController)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeTopBar(
    tabs: List<String>,
    selectedIndex: Int,
    onChatClick: () -> Unit,
    onSearchClick: () -> Unit,
    onTabSelected: (Int) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // AI 聊天入口
        IconButton(onClick = onChatClick) {
            Icon(
                painter = painterResource(R.drawable.ic_chat),
                contentDescription = "聊天",
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.size(20.dp),
            )
        }
        // 关注/发现 tab
        PrimaryTabRow(
            selectedTabIndex = selectedIndex,
            modifier = Modifier.weight(1f),
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onBackground,
            indicator = {
                Box(
                    modifier = Modifier
                        .tabIndicatorOffset(selectedIndex, matchContentSize = true)
                        .offset(y = (-8).dp)
                        .height(2.dp)
                        .clip(RoundedCornerShape(1.5.dp))
                        .background(LimePrimary)
                )
            },
            divider = {},
        ) {
            CompositionLocalProvider(LocalRippleConfiguration provides null) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedIndex == index,
                    onClick = { onTabSelected(index) },
                    text = {
                        Text(
                            text = title,
                            fontSize = 15.sp,
                            fontWeight = if (selectedIndex == index) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedIndex == index)
                                MaterialTheme.colorScheme.onBackground
                            else
                                LimeGray,
                        )
                    },
                )
            }
        }
        }
        // 搜索入口
        IconButton(onClick = onSearchClick) {
            Icon(
                imageVector = Icons.Outlined.Search,
                contentDescription = "搜索",
                tint = MaterialTheme.colorScheme.onBackground,
            )
        }
    }
}

/// 关注页
@Composable
private fun FollowTab(navController: NavHostController) {
    val viewModel: FeedViewModel = hiltViewModel()
    HomeFeedPage(
        navController = navController,
        feed = viewModel.followingFeed,
        emptyHint = "关注的人还没有发布笔记",
    )
}

/// 发现页
@Composable
private fun DiscoverTab(navController: NavHostController) {
    val viewModel: FeedViewModel = hiltViewModel()
    HomeFeedPage(
        navController = navController,
        feed = viewModel.discoverFeed,
    )
}

/// 首页瀑布流信息流
@Composable
private fun HomeFeedPage(
    navController: NavHostController,
    feed: Flow<PagingData<FeedItem>>,
    emptyHint: String? = null,
) {
    val viewModel: FeedViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val gridState = rememberLazyStaggeredGridState()
    val pagingItems = feed.collectAsLazyPagingItems()
    val refreshState = pagingItems.loadState.refresh

    // 弱网预加载
    LaunchedEffect(gridState) {
        val imageLoader = SingletonImageLoader.get(context)
        snapshotFlow { gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 }
            .distinctUntilChanged()
            .collect { lastVisible ->
                pagingItems.itemSnapshotList.items.drop(lastVisible + 1).take(PRELOAD_COUNT)
                    .mapNotNull { it.coverImage }
                    .forEach { url ->
                        imageLoader.enqueue(ImageRequest.Builder(context).data(url).build())
                    }
            }
    }

    Box(modifier = Modifier.fillMaxSize().background(LimeLightGray)) {
        when (refreshState) {
            // 无缓存首屏加载
            is LoadState.Loading if pagingItems.itemCount == 0 -> {
                FeedSkeleton()
            }
            // 无缓存且加载失败
            is LoadState.Error if pagingItems.itemCount == 0 -> {
                ErrorState(
                    message = refreshState.error.message,
                    onRetry = { pagingItems.retry() },
                    modifier = Modifier.align(Alignment.Center),
                )
            }
            // 拉取完成但为空
            is LoadState.NotLoading if emptyHint != null && pagingItems.itemCount == 0 -> {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(horizontal = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = emptyHint,
                        color = LimeGray,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        textAlign = TextAlign.Center,
                    )
                    TextButton(onClick = { pagingItems.refresh() }) {
                        Text(text = "刷新看看", color = LimePrimary)
                    }
                }
            }

            else -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    val pullState = rememberPullToRefreshState()
                    PullToRefreshBox(
                        isRefreshing = refreshState is LoadState.Loading,
                        onRefresh = { pagingItems.refresh() },
                        state = pullState,
                        modifier = Modifier.weight(1f),
                        indicator = {
                            PullToRefreshDefaults.Indicator(
                                state = pullState,
                                isRefreshing = refreshState is LoadState.Loading,
                                containerColor = LimeWhite,
                                color = LimePrimary,
                                modifier = Modifier.align(Alignment.TopCenter),
                            )
                        },
                    ) {
                        PagingWaterfallFeed(
                            pagingItems = pagingItems,
                            likeStates = uiState.likeStates,
                            likeCounts = uiState.likeCounts,
                            state = gridState,
                            onLikeToggle = viewModel::toggleLike,
                            onItemClick = { item ->
                                // 视频笔记进竖屏视频页，图文笔记进详情页
                                if (item.noteType == 2) {
                                    context.openVideo(
                                        item.id,
                                        Screen.VideoFeed.SOURCE_RECOMMENDATION,
                                    )
                                } else {
                                    navController.navigate(
                                        Screen.Detail.createRoute(item.id.toString())
                                    )
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}
