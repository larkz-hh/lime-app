package xyz.larkzhh.lime.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridScope
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import xyz.larkzhh.lime.ui.theme.LimePrimary
import xyz.larkzhh.lime.ui.theme.LimePrimaryPale
import xyz.larkzhh.lime.ui.theme.LimeTheme

// 通用瀑布流组件。
@Composable
fun WaterfallFeed(
    modifier: Modifier = Modifier,
    columns: Int = 2,
    state: LazyStaggeredGridState = rememberLazyStaggeredGridState(),
    contentPadding: PaddingValues = PaddingValues(8.dp),
    isLoadingMore: Boolean = false,
    onLoadMore: () -> Unit = {},
    content: LazyStaggeredGridScope.() -> Unit,
) {
    // 距离末尾4条时触发加载更多
    val shouldLoadMore by remember(state) {
        derivedStateOf {
            val lastVisible = state.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val total = state.layoutInfo.totalItemsCount
            total > 0 && lastVisible >= total - 4
        }
    }
    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) onLoadMore()
    }

    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Fixed(columns),
        state = state,
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalItemSpacing = 8.dp,
    ) {
        content()
        if (isLoadingMore) {
            // 占满整行的 item
            item(span = StaggeredGridItemSpan.FullLine) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = LimePrimary,
                        strokeWidth = 2.dp,
                    )
                }
            }
        }
    }
}
@Preview(showBackground = true, name = "正常状态")
@Composable
private fun WaterfallFeedPreview() {
    LimeTheme {
        WaterfallFeed(
            isLoadingMore = false,
            onLoadMore = {},
        ) {
            items(6) { index ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height((120 + index * 30).dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(LimePrimaryPale),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("Card $index")
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "加载中")
@Composable
private fun WaterfallFeedLoadingPreview() {
    LimeTheme {
        WaterfallFeed(
            isLoadingMore = true,
            onLoadMore = {},
        ) {
            items(4) { index ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(LimePrimaryPale),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("Card $index")
                }
            }
        }
    }
}