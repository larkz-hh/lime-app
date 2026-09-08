package xyz.larkzhh.lime.ui.publish.components

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.saket.telephoto.zoomable.coil3.ZoomableAsyncImage
import xyz.larkzhh.lime.R
import xyz.larkzhh.lime.ui.publish.viewmodel.LocalImage

/// 相册图片全屏预览浮层
@Composable
fun PickerImagePreview(
    images: List<LocalImage>,
    initialIndex: Int,
    selectedUris: List<Uri>,
    onToggle: (Uri) -> Unit,
    onDismiss: () -> Unit,
) {
    BackHandler(onBack = onDismiss)
    val pagerState = rememberPagerState(initialPage = initialIndex) { images.size }

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
                model = images[page].uri,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
            )
        }

        // 顶栏
        val currentUri = images[pagerState.currentPage].uri
        val selectionIndex = selectedUris.indexOf(currentUri)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.3f))
                .statusBarsPadding()
                .padding(horizontal = 4.dp, vertical = 4.dp),
        ) {
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.CenterStart),
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.back),
                    tint = Color.White,
                )
            }
            // 右上角选中圆圈
            SelectionCircle(
                selected = selectionIndex >= 0,
                label = if (selectionIndex >= 0) (selectionIndex + 1).toString() else null,
                onClick = { onToggle(currentUri) },
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 12.dp),
            )
        }
    }
}

/// 预览顶栏右上角选中圆圈
@Composable
internal fun SelectionCircle(
    selected: Boolean,
    label: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(26.dp)
            .clip(CircleShape)
            .then(
                if (selected) {
                    Modifier.background(MaterialTheme.colorScheme.primary)
                } else {
                    Modifier
                        .background(Color.Black.copy(alpha = 0.3f))
                        .border(2.dp, Color.White, CircleShape)
                }
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            if (label != null) {
                Text(
                    text = label,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                )
            } else {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = stringResource(R.string.picker_selected),
                    tint = Color.White,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}
