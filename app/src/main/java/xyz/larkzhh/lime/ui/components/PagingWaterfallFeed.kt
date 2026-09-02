package xyz.larkzhh.lime.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemKey
import xyz.larkzhh.lime.data.network.model.FeedItem
import xyz.larkzhh.lime.ui.theme.LimePrimary

/**
 * 瀑布流列表组件
 */
@Composable
fun PagingWaterfallFeed(
    pagingItems: LazyPagingItems<FeedItem>,
    likeStates: Map<Long, Boolean>,
    likeCounts: Map<Long, Int>,
    onLikeToggle: (FeedItem, Boolean, Int) -> Unit,
    onItemClick: (FeedItem) -> Unit,
    modifier: Modifier = Modifier,
    state: LazyStaggeredGridState = rememberLazyStaggeredGridState(),
    contentPadding: PaddingValues = PaddingValues(start = 5.dp, end = 5.dp, top = 0.dp, bottom = 0.dp),
) {
    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Fixed(2),
        state = state,
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalItemSpacing = 4.dp,
    ) {
        items(
            count = pagingItems.itemCount,
            key = pagingItems.itemKey { it.id },
        ) { index ->
            val item = pagingItems[index] ?: return@items
            val liked = likeStates[item.id] ?: item.liked
            val count = likeCounts[item.id] ?: item.likeCount
            NoteCard(
                item = item.copy(liked = liked, likeCount = count),
                liked = liked,
                onLikeToggle = { onLikeToggle(item, liked, count) },
                onClick = { onItemClick(item) },
            )
        }

        // 触底加载更多、失败重试
        when (val append = pagingItems.loadState.append) {
            is LoadState.Loading -> item(span = StaggeredGridItemSpan.FullLine) {
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
            is LoadState.Error -> item(span = StaggeredGridItemSpan.FullLine) {
                LoadMoreErrorItem(
                    message = append.error.message,
                    onRetry = { pagingItems.retry() },
                )
            }
            else -> Unit
        }
    }
}
