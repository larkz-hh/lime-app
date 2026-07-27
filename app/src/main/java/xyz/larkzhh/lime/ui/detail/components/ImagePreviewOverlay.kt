package xyz.larkzhh.lime.ui.detail.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import xyz.larkzhh.lime.data.network.model.NoteImageData

@Composable
fun ImagePreviewOverlay(
    images: List<NoteImageData>,
    initialIndex: Int,
    onDismiss: () -> Unit,
) {
    BackHandler(onBack = onDismiss)

    val pagerState = rememberPagerState(initialPage = initialIndex) { images.size }
    var isZoomed by remember { mutableStateOf(false) }

    // 换页时重置缩放标记
    LaunchedEffect(pagerState.currentPage) {
        isZoomed = false
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            beyondViewportPageCount = 1,
            userScrollEnabled = !isZoomed,
        ) { page ->
            ZoomableImage(
                url = images[page].url,
                isCurrentPage = page == pagerState.currentPage,
                onTap = onDismiss,
                onScaleChanged = { scale ->
                    if (page == pagerState.currentPage) {
                        isZoomed = scale > 1f
                    }
                },
            )
        }

        // 关闭按钮
        IconButton(
            onClick = onDismiss,
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(4.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "关闭预览",
                tint = Color.White,
                modifier = Modifier.size(24.dp),
            )
        }

        // 多图时显示页码
        if (images.size > 1) {
            Text(
                text = "${pagerState.currentPage + 1}/${images.size}",
                color = Color.White,
                fontSize = 14.sp,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 14.dp)
                    .background(Color.Black.copy(alpha = 0.45f), CircleShape)
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            )
        }
    }
}

/// 支持双指缩放、平移、单击退出的单张图片
/// 非当前页时自动重置缩放
@Composable
private fun ZoomableImage(
    url: String,
    isCurrentPage: Boolean,
    onTap: () -> Unit,
    onScaleChanged: (Float) -> Unit,
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    // 切换到其他页时重置缩放与位移
    LaunchedEffect(isCurrentPage) {
        if (!isCurrentPage) {
            scale = 1f
            offsetX = 0f
            offsetY = 0f
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    val newScale = (scale * zoom).coerceIn(1f, 5f)
                    scale = newScale
                    onScaleChanged(newScale)
                    if (newScale > 1f) {
                        offsetX += pan.x
                        offsetY += pan.y
                    } else {
                        // 缩放回 1 时归位
                        offsetX = 0f
                        offsetY = 0f
                    }
                }
            }
            .pointerInput(Unit) {
                // 轻触触发关闭
                detectTapGestures(onTap = { onTap() })
            },
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(
            model = url,
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offsetX
                    translationY = offsetY
                },
            contentScale = ContentScale.Fit,
        )
    }
}
