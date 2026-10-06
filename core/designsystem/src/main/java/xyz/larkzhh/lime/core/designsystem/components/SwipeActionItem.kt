package xyz.larkzhh.lime.core.designsystem.components

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * 左滑删除列表项
 *
 * @param actionContent 右侧操作按钮内容
 * @param onActionClick 点击操作按钮回调
 * @param actionWidth 操作按钮宽度
 * @param actionColor 操作按钮背景色
 * @param content 前景内容
 */
@Composable
fun SwipeActionItem(
    actionContent: @Composable BoxScope.() -> Unit,
    onActionClick: () -> Unit,
    modifier: Modifier = Modifier,
    actionWidth: Dp = 76.dp,
    actionColor: Color = Color(0xFFFE2C55),
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    val actionWidthPx = with(density) { actionWidth.toPx() }
    val scope = rememberCoroutineScope()
    val offset = remember { Animatable(0f) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clipToBounds(),
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(actionColor),
        ) {
            // 右侧操作按钮区
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(actionWidth)
                    .align(Alignment.CenterEnd)
                    .clickable(onClick = onActionClick),
                contentAlignment = Alignment.Center,
            ) {
                actionContent()
            }
        }

        // 前景
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(offset.value.roundToInt(), 0) }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            scope.launch {
                                offset.snapTo((offset.value + dragAmount).coerceIn(-actionWidthPx, 0f))
                            }
                        },
                        onDragEnd = {
                            scope.launch {
                                val target =
                                    if (offset.value < -actionWidthPx / 2) -actionWidthPx else 0f
                                offset.animateTo(target)
                            }
                        },
                        onDragCancel = {
                            scope.launch { offset.animateTo(0f) }
                        },
                    )
                },
        ) {
            content()
        }
    }
}
