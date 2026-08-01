package xyz.larkzhh.lime.ui.profile.history

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import xyz.larkzhh.lime.data.network.model.toFeedItem
import xyz.larkzhh.lime.navigation.Screen
import xyz.larkzhh.lime.ui.components.SelectableNoteCard
import xyz.larkzhh.lime.ui.components.WaterfallFeed
import xyz.larkzhh.lime.ui.profile.components.HistoryManageBar
import xyz.larkzhh.lime.ui.theme.LimeGray
import xyz.larkzhh.lime.ui.theme.LimePrimary
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowseHistoryScreen(
    navController: NavHostController,
    viewModel: BrowseHistoryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val gridState = rememberLazyStaggeredGridState()

    LaunchedEffect(uiState.deleteError) {
        uiState.deleteError?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearDeleteError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "浏览记录",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            if (uiState.isManaging) viewModel.exitManageMode()
                            else viewModel.enterManageMode()
                        }
                    ) {
                        Text(if (uiState.isManaging) "完成" else "管理")
                    }
                },
            )
        },
        bottomBar = {
            if (uiState.isManaging) {
                HistoryManageBar(
                    selectedCount = uiState.selectedIds.size,
                    totalCount = uiState.items.size,
                    onSelectAll = { viewModel.selectAll() },
                    onDelete = { viewModel.deleteSelected() },
                    isDeleting = uiState.isDeleting,
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
    ) { innerPadding ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(28.dp),
                        color = LimePrimary,
                        strokeWidth = 2.dp,
                    )
                }
            }
            uiState.error != null && uiState.items.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = uiState.error ?: "加载失败",
                        color = LimeGray,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            // 无浏览记录
            uiState.items.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "还没有浏览记录",
                        color = LimeGray,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            else -> {
                WaterfallFeed(
                    modifier = Modifier.padding(innerPadding),
                    state = gridState,
                    isLoadingMore = uiState.isLoadingMore,
                    onLoadMore = viewModel::loadMore,
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
                ) {
                    // 按浏览时间分组，插入日期分组头
                    var lastGroup: String? = null
                    for (historyItem in uiState.items) {
                        val group = computeDateGroup(historyItem.viewTime)// 更新记录
                        if (group != lastGroup) {
                            lastGroup = group
                            // 插入一个日期标题头
                            item(key = "header_$group", span = StaggeredGridItemSpan.FullLine) {
                                Text(
                                    text = group,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(
                                        start = 12.dp,
                                        end = 12.dp,
                                        top = 10.dp,
                                        bottom = 6.dp,
                                    ),
                                )
                            }
                        }
                        val noteId = historyItem.id
                        item(key = noteId) {
                            SelectableNoteCard(
                                item = historyItem.toFeedItem(),
                                liked = noteId in uiState.likedIds,
                                onLikeToggle = { viewModel.toggleLike(noteId) },
                                onClick = {
                                    navController.navigate(Screen.Detail.createRoute(noteId.toString()))
                                },
                                isSelectMode = uiState.isManaging,
                                isSelected = noteId in uiState.selectedIds,
                                onToggleSelect = { viewModel.toggleSelect(noteId) },
                                modifier = Modifier.padding(4.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

/// 根据浏览时间字符串计算分组标签
private fun computeDateGroup(viewTime: String): String = try {
    val itemDate = LocalDate.parse(viewTime.take(10))
    val today = LocalDate.now()
    when {
        itemDate == today -> "今天"
        itemDate == today.minusDays(1) -> "昨天"
        itemDate >= today.minusDays(7) -> "一周内"
        else -> "更早"
    }
} catch (e: Exception) {
    "更早"
}