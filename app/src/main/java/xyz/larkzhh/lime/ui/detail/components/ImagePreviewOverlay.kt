package xyz.larkzhh.lime.ui.detail.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import me.saket.telephoto.zoomable.coil3.ZoomableAsyncImage
import xyz.larkzhh.lime.ui.components.BottomActionSheet
import xyz.larkzhh.lime.ui.components.SheetAction
import xyz.larkzhh.lime.ui.theme.LimeGray
import xyz.larkzhh.lime.util.saveImageToGallery
import xyz.larkzhh.lime.util.showToast

/// 全屏图片预览浮层
/// 支持左右滑动翻页、双指/双击缩放、拖动到边缘自动翻页
/// 单击退出，长按弹出操作菜单
@Composable
fun ImagePreviewOverlay(
    images: List<String>,
    initialIndex: Int,
    onDismiss: () -> Unit,
) {
    BackHandler(onBack = onDismiss)
    val pagerState = rememberPagerState(initialPage = initialIndex) { images.size }
    var showSheet by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            beyondViewportPageCount = 1,
        ) { page ->
            ZoomableAsyncImage(
                model = images[page],
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
                onClick = { onDismiss() },
                onLongClick = { showSheet = true },
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

        // 长按弹出底部操作菜单
        BottomActionSheet(
            visible = showSheet,
            onDismiss = { showSheet = false },
            actions = listOf(
                SheetAction(
                    label = "保存图片",
                    onClick = {
                        val url = images[pagerState.currentPage]
                        scope.launch {
                            val ok = saveImageToGallery(context, url)
                            val text = if (ok) "已保存到相册" else "保存失败"
                            text.showToast(context)
                        }
                    },
                ),
                SheetAction(
                    label = "取消",
                    textColor = LimeGray,
                    onClick = {},
                ),
            ),
        )
    }
}