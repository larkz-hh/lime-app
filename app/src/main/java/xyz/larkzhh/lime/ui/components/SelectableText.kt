package xyz.larkzhh.lime.ui.components

import android.icu.text.BreakIterator
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import xyz.larkzhh.lime.ui.theme.LimePrimary
import java.util.Locale
import kotlin.math.roundToInt

private val ItemWidth = 52.dp// 浮层项固定宽度
private val ToolbarHeight = 52.dp// 浮层固定高度
private val ToolbarVerticalPadding = 6.dp// 垂直内边距
private val HandleRadius = 6.dp// 选区拖杆小球半径

data class SelectionAction(
    val label: String,
    val icon: ImageVector,
    val onClick: (String) -> Unit,
)

/**
 * 支持长按选词的文本
 *
 * @param text 文本内容
 * @param actions 浮层里的选项列表
 * @param modifier 外部传入的 Modifier
 * @param style 文本样式
 * @param highlightColor 选中词的底色
 * @param handleColor 选区两端拖杆的颜色
 * @param toolbarColor 浮层背景色
 * @param toolbarContentColor 浮层图标与文字颜色
 */
@Composable
fun SelectableText(
    text: String,
    actions: List<SelectionAction>,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    highlightColor: Color = LimePrimary.copy(alpha = 0.3f),
    handleColor: Color = LimePrimary,
    toolbarColor: Color = Color(0xFF2C2C2E),
    toolbarContentColor: Color = Color.White,
) {
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current// 获取当前屏幕密度
    val windowWidth = with(density) { LocalConfiguration.current.screenWidthDp.dp.roundToPx() }
    val gapPx = with(density) { 8.dp.roundToPx() }
    val radiusPx = with(density) { HandleRadius.roundToPx() }
    val toolbarHeightPx = with(density) { (ToolbarHeight + ToolbarVerticalPadding * 2).roundToPx() }
    val toolbarWidthPx = with(density) { (ItemWidth * actions.size + 8.dp).roundToPx() }

    var layout by remember(text) { mutableStateOf<TextLayoutResult?>(null) }//记录 Text 组件在窗口中的绝对坐标原点
    var origin by remember { mutableStateOf(Offset.Zero) }// Text 组件在窗口中的绝对坐标原点
    var selection by remember(text) { mutableStateOf<TextRange?>(null) }// 当前选中的文本范围

    val display = remember(text, selection, highlightColor) {
        val range = selection ?: return@remember AnnotatedString(text)
        buildAnnotatedString {
            append(text.substring(0, range.start))
            // 开启高亮样式块
            withStyle(SpanStyle(background = highlightColor)) {
                append(text.substring(range.start, range.end))
            }
            append(text.substring(range.end))
        }
    }

    // 拖杆拖动时的实时位置，窗口坐标
    var dragAt by remember { mutableStateOf(Offset.Zero) }

    /// 把窗口坐标换算成字符位置
    fun moveEdge(isStart: Boolean) {
        val result = layout ?: return
        val range = selection ?: return
        val offset = result.getOffsetForPosition(dragAt - origin)//  将相对坐标转换为字符索引
        selection = if (isStart) {
            if (offset < range.end) TextRange(offset, range.end) else range
        } else {
            if (offset > range.start) TextRange(range.start, offset) else range
        }
    }

    Box {
        Text(
            text = display,
            style = style,
            onTextLayout = { layout = it },
            modifier = modifier
                .onGloballyPositioned { origin = it.positionInWindow() }
                .pointerInput(text) {
                    detectTapGestures(
                        onTap = { selection = null },// 取消选中
                        onLongPress = { position ->
                            val result = layout ?: return@detectTapGestures
                            selection = wordRangeAt(text, result.getOffsetForPosition(position))
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)// 长按震动
                        },
                    )
                },
        )
        val range = selection
        val result = layout
        if (range != null && result != null) {
            val anchor = selectionBounds(result, range).translate(origin)
            // 浮层贴选区上沿，顶部空间不够时翻到下沿
            val above = anchor.top.roundToInt() - toolbarHeightPx - gapPx
            val toolbarTop = if (above >= 0) above else anchor.bottom.roundToInt() + gapPx
            // 弹窗覆盖浮层与两端拖杆，纵向留出拖杆小球
            val bandTop = minOf(toolbarTop, anchor.top.roundToInt() - radiusPx)
            val bandBottom = maxOf(
                toolbarTop + toolbarHeightPx,
                anchor.bottom.roundToInt() + radiusPx * 2,
            )

            Popup(
                popupPositionProvider = remember(bandTop) { BandPosition(bandTop) },
                onDismissRequest = { selection = null },// 点击弹窗外部时取消选中
                properties = PopupProperties(focusable = true),// 允许弹窗获取焦点
            ) {
                Box(
                    modifier = Modifier
                        .width(with(density) { windowWidth.toDp() })
                        .height(with(density) { (bandBottom - bandTop).toDp() })
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() },
                            onClick = { selection = null },// 点击遮罩带内的空白区域，取消选中
                        ),
                ) {
                    val toolbarLeft = (anchor.center.x - toolbarWidthPx / 2f).roundToInt()
                        .coerceIn(0, (windowWidth - toolbarWidthPx).coerceAtLeast(0))
                    Row(
                        modifier = Modifier
                            .offset {
                                IntOffset(toolbarLeft, toolbarTop - bandTop)// 遮罩带内部的相对位置
                            }
                            .background(toolbarColor, RoundedCornerShape(10.dp))
                            .padding(horizontal = 4.dp, vertical = ToolbarVerticalPadding),
                        horizontalArrangement = Arrangement.spacedBy(0.dp),
                    ) {
                        actions.forEach { action ->
                            Column(
                                modifier = Modifier
                                    .width(ItemWidth)
                                    .height(ToolbarHeight)
                                    .clickable(
                                        indication = null,
                                        interactionSource = remember { MutableInteractionSource() },
                                    ) {
                                        val selected = text.substring(range.start, range.end)// 选中的文本
                                        selection = null
                                        action.onClick(selected)
                                    },
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                            ) {
                                Icon(
                                    imageVector = action.icon,
                                    contentDescription = action.label,
                                    tint = toolbarContentColor,
                                    modifier = Modifier.size(19.dp),
                                )
                                Text(
                                    text = action.label,
                                    color = toolbarContentColor,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(top = 3.dp),
                                )
                            }
                        }
                    }

                    // 选区左端拖杆
                    SelectionHandle(
                        color = handleColor,
                        center = IntOffset(
                            anchor.left.roundToInt(),
                            anchor.top.roundToInt() - bandTop,
                        ),
                        onDragStart = { dragAt = origin + Offset(anchor.left, anchor.center.y) },
                        onDrag = { delta ->
                            dragAt += delta
                            moveEdge(isStart = true)// 把最新的 dragAt 转换成字符索引
                        },
                    )

                    // 选区右端拖杆
                    SelectionHandle(
                        color = handleColor,
                        center = IntOffset(
                            anchor.right.roundToInt(),
                            anchor.bottom.roundToInt() - bandTop,
                        ),
                        onDragStart = { dragAt = origin + Offset(anchor.right, anchor.center.y) },
                        onDrag = { delta ->
                            dragAt += delta
                            moveEdge(isStart = false)
                        },
                    )
                }
            }
        }
    }
}

/// 选区一端的拖杆
@Composable
private fun SelectionHandle(
    color: Color,
    center: IntOffset,
    onDragStart: () -> Unit,
    onDrag: (Offset) -> Unit,
) {
    val density = LocalDensity.current
    val touchRadius = 16.dp
    val touchRadiusPx = with(density) { touchRadius.roundToPx() }

    Box(
        modifier = Modifier
            .offset { IntOffset(center.x - touchRadiusPx, center.y - touchRadiusPx) }
            .size(touchRadius * 2)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { onDragStart() },
                    onDrag = { _, delta -> onDrag(delta) },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(HandleRadius * 2)
                .background(color, CircleShape),
        )
    }
}

/// 用系统 ICU 词典切出 offset 所在的词，切不出有效内容时退化为单个字
private fun wordRangeAt(text: String, offset: Int): TextRange {
    if (text.isEmpty()) return TextRange.Zero
    val target = offset.coerceIn(0, text.length - 1)
    val single = TextRange(target, target + 1)// 找不到完整的单词，选中这一个字符

    val iterator = BreakIterator.getWordInstance(Locale.CHINESE)// 中文语言环境
    iterator.setText(text)
    val end = iterator.following(target).let { if (it == BreakIterator.DONE) text.length else it }// 右边界
    val start = iterator.previous().let { if (it == BreakIterator.DONE) 0 else it }// 左边界
    if (start >= end) return single
    return if (text.substring(start, end).isBlank()) single else TextRange(start, end)
}

/// 选区的矩形包围盒
private fun selectionBounds(layout: TextLayoutResult, range: TextRange): Rect {
    val head = layout.getBoundingBox(range.start)
    val tail = layout.getBoundingBox((range.end - 1).coerceAtLeast(range.start))
    return Rect(
        left = minOf(head.left, tail.left),
        top = minOf(head.top, tail.top),
        right = maxOf(head.right, tail.right),
        bottom = maxOf(head.bottom, tail.bottom),
    )
}

// 弹窗定位
private class BandPosition(private val topPx: Int) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset = IntOffset(0, topPx)
}
