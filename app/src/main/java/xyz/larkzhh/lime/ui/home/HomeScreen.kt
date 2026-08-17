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
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import kotlinx.coroutines.launch
import xyz.larkzhh.lime.navigation.Screen
import xyz.larkzhh.lime.ui.components.NoteCard
import xyz.larkzhh.lime.ui.components.WaterfallFeed
import xyz.larkzhh.lime.ui.theme.LimeGray
import xyz.larkzhh.lime.ui.theme.LimeLightGray
import xyz.larkzhh.lime.ui.theme.LimePrimary
import xyz.larkzhh.lime.ui.theme.LimeWhite

@Composable
fun HomeScreen(navController: NavHostController) {
    val tabs = listOf("关注", "发现")
    val pagerState = rememberPagerState { tabs.size }
    val coroutineScope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize()) {
        HomeTopBar(
            tabs = tabs,
            selectedIndex = pagerState.currentPage,
            onChatClick = { /* TODO: AI 聊天 */ },
            onSearchClick = { navController.navigate(Screen.Search.route) },
            onTabSelected = { index ->
                coroutineScope.launch { pagerState.animateScrollToPage(index) }
            },
        )
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
        ) { page ->
            when (page) {
                0 -> FollowTab()
                1 -> DiscoverTab(navController)
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
                imageVector = Icons.Outlined.ChatBubbleOutline,
                contentDescription = "聊天",
                tint = MaterialTheme.colorScheme.onBackground,
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
private fun FollowTab() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LimeLightGray),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = "关注", color = LimeGray)
    }
}

/// 发现页
@Composable
private fun DiscoverTab(navController: NavHostController) {
    val viewModel: FeedViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsState()

    Box(modifier = Modifier.fillMaxSize().background(LimeLightGray)) {
        when {
            uiState.isLoading -> {
                // 首次加载
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(32.dp)
                        .align(Alignment.Center),
                    color = LimePrimary,
                    trackColor = LimeWhite,
                    strokeWidth = 2.dp,
                )
            }
            uiState.error != null && uiState.items.isEmpty() -> {
                Text(
                    text = uiState.error ?: "加载失败",
                    modifier = Modifier.align(Alignment.Center),
                    color = LimeGray,
                )
            }
            else -> {
                // 下拉刷新
                val refreshState = rememberPullToRefreshState()
                PullToRefreshBox(
                    isRefreshing = uiState.isRefreshing,
                    onRefresh = viewModel::refresh,
                    state = refreshState,
                    modifier = Modifier.fillMaxSize(),
                    indicator = {
                        PullToRefreshDefaults.Indicator(
                            state = refreshState,
                            isRefreshing = uiState.isRefreshing,
                            containerColor = LimeWhite,
                            color = LimePrimary,
                            modifier = Modifier.align(Alignment.TopCenter),
                        )
                    },
                ) {
                    WaterfallFeed(
                        isLoadingMore = uiState.isLoadingMore,
                        onLoadMore = viewModel::loadMore,
                    ) {
                        items(uiState.items, key = { it.id }) { item ->
                            NoteCard(
                                item = item,
                                liked = item.id in uiState.likedIds,
                                onLikeToggle = { viewModel.toggleLike(item.id) },
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
        }
    }
}
