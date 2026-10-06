package xyz.larkzhh.lime.ui.video.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

/**
 * 竖直拉伸调节柱
 *
 * @param fraction 当前值
 * @param icon 中央图标
 * @param onFractionChange 拖动回调
 */
@Composable
fun VerticalSlider(
    fraction: Float,
    icon: Painter,
    onFractionChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val barHeight = 180.dp
    val barWidth = 48.dp
    Box(
        modifier = modifier
            .width(barWidth)
            .height(barHeight)
            .clip(RoundedCornerShape(barWidth / 2))
            .background(Color.Black.copy(alpha = 0.4f))
            .pointerInput(Unit) {
                detectVerticalDragGestures { change, _ ->
                    change.consume()
                    val f = 1f - (change.position.y / size.height)
                    onFractionChange(f.coerceIn(0f, 1f))
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        // 已填充部分
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(fraction.coerceIn(0f, 1f))
                .align(Alignment.BottomCenter)
                .background(Color.White.copy(alpha = 0.9f)),
        )
        // 中央图标
        Icon(
            painter = icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(24.dp),
        )
    }
}
