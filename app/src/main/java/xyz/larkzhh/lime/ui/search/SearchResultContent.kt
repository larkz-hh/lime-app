package xyz.larkzhh.lime.ui.search

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import xyz.larkzhh.lime.ui.components.NoteCard
import xyz.larkzhh.lime.ui.components.WaterfallFeed
import xyz.larkzhh.lime.ui.search.viewmodel.NoteSort
import xyz.larkzhh.lime.ui.search.viewmodel.SearchTimeRange
import xyz.larkzhh.lime.ui.search.viewmodel.SearchUiState
import xyz.larkzhh.lime.ui.theme.LimeDark
import xyz.larkzhh.lime.ui.theme.LimeGray
import xyz.larkzhh.lime.ui.theme.LimeLightGray
import xyz.larkzhh.lime.ui.theme.LimePrimary
import xyz.larkzhh.lime.ui.theme.LimePrimaryPale
import xyz.larkzhh.lime.ui.theme.LimeWhite

@Composable
fun SearchResultContent(
    uiState: SearchUiState,
    onLoadMore: () -> Unit,
    onLikeToggle: (Long) -> Unit,
    onNoteClick: (Long) -> Unit,
    onSortChange: (NoteSort) -> Unit,
    onTimeRangeChange: (SearchTimeRange) -> Unit,
    onResetFilter: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tabs = listOf("全部", "用户")
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var filterExpanded by rememberSaveable { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize()) {
        // tab 栏
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            tabs.forEachIndexed { index, title ->
                val selected = selectedTab == index
                Column(
                    modifier = Modifier
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) {
                            if (index == 0 && selectedTab == 0) {
                                // 展开/收起筛选抽屉
                                filterExpanded = !filterExpanded
                            } else {
                                selectedTab = index
                                if (index != 0) filterExpanded = false
                            }
                        }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = title,
                            fontSize = 15.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            color = if (selected) MaterialTheme.colorScheme.onBackground else LimeGray,
                        )
                        // 筛选
                        if (index == 0) {
                            Icon(
                                imageVector = if (filterExpanded) Icons.Outlined.KeyboardArrowUp
                                              else Icons.Outlined.KeyboardArrowDown,
                                contentDescription = "筛选",
                                tint = if (selected) MaterialTheme.colorScheme.onBackground else LimeGray,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .width(20.dp)
                            .height(2.dp)
                            .background(if (selected) LimePrimary else Color.Transparent),
                    )
                }
            }
        }
        HorizontalDivider(thickness = 0.5.dp, color = LimeLightGray)

        // 内容区
        Box(modifier = Modifier.fillMaxSize()) {
            when (selectedTab) {
                // 搜索笔记内容
                0 -> NoteResultList(
                    uiState = uiState,
                    onLoadMore = onLoadMore,
                    onLikeToggle = onLikeToggle,
                    onNoteClick = onNoteClick,
                )
                // 搜索用户内容
                1 -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = "用户搜索施工中", color = LimeGray, fontSize = 14.sp)
                }
            }

            FilterOverlay(
                visible = filterExpanded,
                uiState = uiState,
                onSortChange = onSortChange,
                onTimeRangeChange = onTimeRangeChange,
                onReset = {
                    onResetFilter()
                    filterExpanded = false
                },
                onCollapse = { filterExpanded = false },
            )
        }
    }
}

/// 筛选抽屉覆盖层
@Composable
private fun BoxScope.FilterOverlay(
    visible: Boolean,
    uiState: SearchUiState,
    onSortChange: (NoteSort) -> Unit,
    onTimeRangeChange: (SearchTimeRange) -> Unit,
    onReset: () -> Unit,
    onCollapse: () -> Unit,
) {
    // 遮罩（点击收起抽屉）
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.4f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) { onCollapse() },
        )
    }
    // 筛选抽屉
    AnimatedVisibility(
        visible = visible,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut(),
        modifier = Modifier.align(Alignment.TopCenter),
    ) {
        SearchFilterPanel(
            sort = uiState.sort,
            timeRange = uiState.timeRange,
            onSortChange = onSortChange,
            onTimeRangeChange = onTimeRangeChange,
            onReset = onReset,
            onCollapse = onCollapse,
        )
    }
}

/// 笔记搜索结果瀑布流
@Composable
private fun NoteResultList(
    uiState: SearchUiState,
    onLoadMore: () -> Unit,
    onLikeToggle: (Long) -> Unit,
    onNoteClick: (Long) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LimeLightGray),
    ) {
        when {
            uiState.isResultLoading -> {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(32.dp)
                        .align(Alignment.Center),
                    color = LimePrimary,
                    trackColor = LimeWhite,
                    strokeWidth = 2.dp,
                )
            }
            uiState.resultError != null && uiState.resultItems.isEmpty() -> {
                Text(
                    text = uiState.resultError ?: "加载失败",
                    modifier = Modifier.align(Alignment.Center),
                    color = LimeGray,
                )
            }
            uiState.resultItems.isEmpty() -> {
                Text(
                    text = "暂无相关笔记",
                    modifier = Modifier.align(Alignment.Center),
                    color = LimeGray,
                )
            }
            else -> {
                WaterfallFeed(
                    isLoadingMore = uiState.isLoadingMore,
                    onLoadMore = onLoadMore,
                ) {
                    items(uiState.resultItems, key = { it.id }) { item ->
                        NoteCard(
                            item = item,
                            liked = item.id in uiState.likedIds,
                            onLikeToggle = { onLikeToggle(item.id) },
                            onClick = { onNoteClick(item.id) },
                        )
                    }
                }
            }
        }
    }
}

/// 筛选抽屉面板
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SearchFilterPanel(
    sort: NoteSort,
    timeRange: SearchTimeRange,
    onSortChange: (NoteSort) -> Unit,
    onTimeRangeChange: (SearchTimeRange) -> Unit,
    onReset: () -> Unit,
    onCollapse: () -> Unit,
) {
    Surface(color = MaterialTheme.colorScheme.surface) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "排序依据",
                    fontSize = 14.sp,
                    color = LimeGray,
                )
                Spacer(modifier = Modifier.height(10.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    NoteSort.entries.forEach { option ->
                        FilterChip(
                            text = option.label,
                            selected = sort == option,
                            onClick = { onSortChange(option) },
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "发布时间",
                    fontSize = 14.sp,
                    color = LimeGray,
                )
                Spacer(modifier = Modifier.height(10.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    SearchTimeRange.entries.forEach { option ->
                        FilterChip(
                            text = option.label,
                            selected = timeRange == option,
                            onClick = { onTimeRangeChange(option) },
                        )
                    }
                }
            }
            HorizontalDivider(thickness = 0.5.dp, color = LimeLightGray)
            // 重置、收起
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(onClick = onReset)
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Refresh,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "重置", fontSize = 14.sp, color = MaterialTheme.colorScheme.onBackground)
                }
                Box(
                    modifier = Modifier
                        .width(0.5.dp)
                        .height(20.dp)
                        .background(LimeLightGray),
                )
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(onClick = onCollapse)
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.KeyboardArrowUp,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "收起", fontSize = 14.sp, color = MaterialTheme.colorScheme.onBackground)
                }
            }
        }
    }
}
/// 筛选项 chip
@Composable
private fun FilterChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(6.dp),
        color = if (selected) LimePrimaryPale else LimeLightGray,
    ) {
        Text(
            text = text,
            fontSize = 13.sp,
            color = if (selected) LimePrimary else LimeDark,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
        )
    }
}
