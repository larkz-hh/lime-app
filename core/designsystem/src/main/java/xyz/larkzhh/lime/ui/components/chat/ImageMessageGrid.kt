package xyz.larkzhh.lime.ui.components.chat

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage

/// 消息图片
@Composable
fun ImageMessageGrid(
    images: List<String>,
    modifier: Modifier = Modifier,
    onImageClick: (index: Int, images: List<String>) -> Unit = { _, _ -> },
) {
    when (images.size) {
        0 -> Unit
        1 -> SingleImage(images[0], modifier, onClick = { onImageClick(0, images) })
        2 -> ImageRow(images, cellSize = 130.dp, modifier, onClick = onImageClick)
        3 -> ImageRow(images, cellSize = 104.dp, modifier, onClick = onImageClick)
        else -> Column(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            ImageRow(images.take(2), cellSize = 112.dp, onClick = { i, _ -> onImageClick(i, images) })
            ImageRow(images.drop(2), cellSize = 112.dp, onClick = { i, _ -> onImageClick(i + 2, images) })
        }
    }
}

/**
 * 单行横滑图片
 */
@Composable
fun HorizontalImageRow(
    images: List<String>,
    modifier: Modifier = Modifier,
    maxWidth: androidx.compose.ui.unit.Dp = 296.dp,
    imageSize: androidx.compose.ui.unit.Dp = 116.dp,
    onImageClick: (index: Int, images: List<String>) -> Unit = { _, _ -> },
) {
    Row(
        modifier = modifier
            .widthIn(max = maxWidth)
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        images.forEachIndexed { index, url ->
            GridImage(
                url = url,
                modifier = Modifier.size(imageSize),
                radius = 12.dp,
                onClick = { onImageClick(index, images) },
            )
        }
    }
}

@Composable
private fun SingleImage(
    url: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    GridImage(url, modifier = modifier.size(190.dp), radius = 12.dp, onClick = onClick)
}

@Composable
private fun ImageRow(
    images: List<String>,
    cellSize: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier,
    onClick: (index: Int, images: List<String>) -> Unit,
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        images.forEachIndexed { index, url ->
            GridImage(
                url = url,
                modifier = Modifier.size(cellSize),
                radius = 8.dp,
                onClick = { onClick(index, images) },
            )
        }
    }
}

@Composable
private fun GridImage(
    url: String,
    modifier: Modifier = Modifier,
    radius: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit,
) {
    AsyncImage(
        model = url,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier
            .clip(RoundedCornerShape(radius))
            .clickable(onClick = onClick),
    )
}
