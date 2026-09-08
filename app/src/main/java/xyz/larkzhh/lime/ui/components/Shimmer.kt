package xyz.larkzhh.lime.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import xyz.larkzhh.lime.ui.theme.LimeLightGray
import xyz.larkzhh.lime.ui.theme.LimeTheme
import xyz.larkzhh.lime.ui.theme.LimeWhite

fun Modifier.shimmer(
    baseColor: Color? = null,
    highlightColor: Color = LimeWhite,
): Modifier = composed {
    // null 跟随当前主题占位底色
    val resolvedBase = baseColor ?: MaterialTheme.colorScheme.surfaceVariant
    val transition = rememberInfiniteTransition(label = "shimmer")
    val progress = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmerProgress",
    )
    drawBehind {
        val p = progress.value
        val gradientWidth = size.width * 0.6f
        val x = -gradientWidth + p * (size.width + gradientWidth)
        val brush = Brush.linearGradient(
            colors = listOf(resolvedBase, highlightColor, resolvedBase),
            start = Offset(x, 0f),
            end = Offset(x + gradientWidth, size.height),
        )
        drawRect(brush)
    }
}

/// 流光占位块
@Composable
fun ShimmerBox(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(8.dp),
) {
    Box(modifier = modifier.clip(shape).shimmer())
}

private val skeletonRatios = listOf(0.75f, 1.0f, 0.8f, 1.15f, 0.7f, 0.95f)

/**
 * 首页信息流骨架屏
 */
@Composable
fun FeedSkeleton(
    modifier: Modifier = Modifier,
    columns: Int = 2,
) {
    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Fixed(columns),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 5.dp, end = 5.dp, top = 0.dp, bottom = 0.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalItemSpacing = 4.dp,
        userScrollEnabled = false,
    ) {
        items(skeletonRatios.size * 3) { index ->
            NoteCardSkeleton(ratio = skeletonRatios[index % skeletonRatios.size])
        }
    }
}

@Composable
private fun NoteCardSkeleton(ratio: Float) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column {
            ShimmerBox(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(ratio)
                    .heightIn(max = MaxCoverHeight),
                shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp),
            )
            Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
                ShimmerBox(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .height(12.dp),
                    shape = RoundedCornerShape(4.dp),
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ShimmerBox(modifier = Modifier.size(18.dp), shape = CircleShape)
                    Spacer(modifier = Modifier.width(4.dp))
                    ShimmerBox(
                        modifier = Modifier
                            .fillMaxWidth(0.5f)
                            .height(10.dp),
                        shape = RoundedCornerShape(4.dp),
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "信息流骨架屏", heightDp = 600)
@Composable
private fun FeedSkeletonPreview() {
    LimeTheme {
        FeedSkeleton()
    }
}

@Preview(showBackground = true, name = "流光占位块")
@Composable
private fun ShimmerBoxPreview() {
    LimeTheme {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ShimmerBox(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
            )
            ShimmerBox(
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(60.dp),
                shape = CircleShape,
            )
        }
    }
}
