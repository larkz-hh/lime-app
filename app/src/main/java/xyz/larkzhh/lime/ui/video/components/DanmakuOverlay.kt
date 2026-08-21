package xyz.larkzhh.lime.ui.video.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import xyz.larkzhh.lime.data.network.model.DanmakuData
import xyz.larkzhh.lime.util.copyToClipboard
import xyz.larkzhh.lime.util.showToast
import androidx.core.graphics.toColorInt

private const val DANMAKU_LANES = 3
private const val DANMAKU_DURATION_MS = 8000L// 一条弹幕穿过的时长
private val LANE_HEIGHT = 22.dp
private val DANMAKU_FONT_SIZE = 14.sp
private val DANMAKU_GAP = 16.dp// 同轨相邻弹幕最小间隔
private val DANMAKU_HIT_PADDING = 8.dp// 弹幕点击命中外扩量

/**
 * 顶部弹幕区
 *
 * 单 Canvas 绘制：逐帧只在绘制阶段读取播放进度，不触发重组；宽度用 TextMeasurer 预先量好，
 * 按「轨道空闲检测」分轨避免重叠。点击命中在 pointerInput 里按坐标反查，未命中不消费、穿透到视频层。
 *
 * @param danmakuList 当前视频弹幕
 * @param player 播放器
 * @param enabled 弹幕开关
 * @param currentUserId 当前登录用户
 * @param noteAuthorId 视频作者
 * @param pausedDanmakuId 当前被冻结的弹幕 id
 * @param frozenMs 冻结时刻的播放进度
 * @param onDanmakuClick 点击某条弹幕
 * @param onDismissBubble 关闭气泡
 * @param onDelete 删除回调
 */
@UnstableApi
@Composable
fun DanmakuOverlay(
    danmakuList: List<DanmakuData>,
    player: ExoPlayer?,
    enabled: Boolean,
    currentUserId: Long?,
    noteAuthorId: Long?,
    pausedDanmakuId: Long?,
    frozenMs: Long,
    onDanmakuClick: (danmakuId: Long, nowMs: Long) -> Unit,
    onDismissBubble: () -> Unit,
    onDelete: (DanmakuData) -> Unit,
    modifier: Modifier = Modifier,
) {
    val regionHeight = LANE_HEIGHT * DANMAKU_LANES

    if (!enabled) {
        Spacer(modifier = modifier.height(regionHeight))
        return
    }

    val context = LocalContext.current
    val textMeasurer = rememberTextMeasurer()

    // 逐帧读取播放进度
    var nowMs by remember { mutableLongStateOf(0L) }
    LaunchedEffect(player) {
        while (true) {
            withFrameMillis { }
            player?.let { nowMs = it.currentPosition }
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .height(regionHeight),
    ) {
        val containerW = constraints.maxWidth.toFloat()
        val density = LocalDensity.current
        val laneHeightPx = with(density) { LANE_HEIGHT.toPx() }
        val gapPx = with(density) { DANMAKU_GAP.toPx() }

        // 测量宽度，分配空闲轨道
        val placed = remember(danmakuList, containerW) {
            layoutDanmaku(danmakuList, containerW, gapPx, textMeasurer)
        }

        Canvas(
            modifier = Modifier
                .zIndex(0f)
                .fillMaxSize()
                .pointerInput(placed, containerW, laneHeightPx) {
                    val hitPad = DANMAKU_HIT_PADDING.toPx()
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val up = waitForUpOrCancellation() ?: return@awaitEachGesture
                        val clock = nowMs
                        val hit = placed.firstOrNull { p ->
                            val frozen = pausedDanmakuId == p.data.id
                            val c = if (frozen) frozenMs else clock
                            val progress = (c - p.data.videoTimeMs).toFloat() / DANMAKU_DURATION_MS
                            if (!frozen && progress !in 0f..1f) return@firstOrNull false
                            val x = containerW - progress * (containerW + p.width)
                            val y = p.lane * laneHeightPx
                            down.position.x in (x - hitPad)..(x + p.width + hitPad) &&
                                down.position.y in (y - hitPad)..(y + laneHeightPx + hitPad)
                        }
                        if (hit != null) {
                            up.consume()
                            onDanmakuClick(hit.data.id, clock)
                        }
                    }
                },
        ) {
            placed.forEach { p ->
                if (pausedDanmakuId == p.data.id) return@forEach
                val progress = (nowMs - p.data.videoTimeMs).toFloat() / DANMAKU_DURATION_MS
                if (progress !in 0f..1f) return@forEach// 不在窗口内
                drawPlaced(p, progress, containerW, laneHeightPx)
            }
            // 冻结弹幕置顶重画
            placed.firstOrNull { it.data.id == pausedDanmakuId }?.let { p ->
                val progress = (frozenMs - p.data.videoTimeMs).toFloat() / DANMAKU_DURATION_MS
                drawPlaced(p, progress, containerW, laneHeightPx)
            }
        }

        // 气泡菜单
        val paused = placed.firstOrNull { it.data.id == pausedDanmakuId }
        if (paused != null) {
            val d = paused.data
            val w = paused.width
            val progress = (frozenMs - d.videoTimeMs).toFloat() / DANMAKU_DURATION_MS
            val danmakuX = containerW - progress * (containerW + w)
            val centerX = danmakuX + w / 2f
            val laneTopY = paused.lane * laneHeightPx

            val canDelete = currentUserId != null &&
                (currentUserId == d.author.id || currentUserId == noteAuthorId)// 删除权限

            var bubbleW by remember(d.id) { mutableIntStateOf(0) }
            var bubbleH by remember(d.id) { mutableIntStateOf(0) }
            val bubbleX = (centerX - bubbleW / 2f).coerceIn(0f, (containerW - bubbleW).coerceAtLeast(0f))
            val bubbleY = laneTopY + laneHeightPx

            Box(
                modifier = Modifier
                    .zIndex(2f)
                    .offset { IntOffset(bubbleX.toInt(), bubbleY.toInt()) }
                    .graphicsLayer { alpha = if (bubbleW == 0) 0f else 1f }
                    .onSizeChanged {
                        bubbleW = it.width
                        bubbleH = it.height
                    },
            ) {
                DanmakuBubble(
                    canDelete = canDelete,
                    arrowCenterX = (centerX - bubbleX),
                    onCopy = {
                        d.content.copyToClipboard(context)
                        "已复制".showToast(context)
                        onDismissBubble()
                    },
                    onDelete = {
                        onDelete(d)
                        onDismissBubble()
                    },
                )
            }
        }
    }
}

/// 冻结气泡
@Composable
private fun DanmakuBubble(
    canDelete: Boolean,
    arrowCenterX: Float,
    onCopy: () -> Unit,
    onDelete: () -> Unit,
) {
    val pill = Color(0xFF2C2C2C)
    androidx.compose.foundation.layout.Column(horizontalAlignment = Alignment.Start) {
        // 上方箭头
        Box(
            modifier = Modifier
                .offset { IntOffset((arrowCenterX - 5.dp.toPx()).toInt(), 5.dp.roundToPx()) }
                .size(10.dp)
                .rotate(45f)
                .background(pill),
        )
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(pill)
                .padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            BubbleAction(icon = Icons.Outlined.ContentCopy, label = "复制", onClick = onCopy)
            if (canDelete) {
                BubbleAction(icon = Icons.Outlined.Delete, label = "删除", onClick = onDelete)
            }
        }
    }
}

@Composable
private fun BubbleAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClick,
            )
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = Color.White,
            modifier = Modifier.size(16.dp),
        )
        Text(text = label, color = Color.White, fontSize = 12.sp)
    }
}

/// 已测量与分配的弹幕
private data class PlacedDanmaku(
    val data: DanmakuData,
    val layout: TextLayoutResult,
    val width: Float,
    val lane: Int,
)

/// 画一条弹幕
@UnstableApi
private fun DrawScope.drawPlaced(
    p: PlacedDanmaku,
    progress: Float,
    containerW: Float,
    laneHeightPx: Float,
) {
    val x = containerW - progress * (containerW + p.width)
    val y = p.lane * laneHeightPx + (laneHeightPx - p.layout.size.height) / 2f
    drawText(p.layout, topLeft = Offset(x, y))
}

/// 预量宽度与轨道分配
private fun layoutDanmaku(
    list: List<DanmakuData>,
    containerW: Float,
    gapPx: Float,
    measurer: TextMeasurer,
): List<PlacedDanmaku> {
    val sorted = list.sortedBy { it.videoTimeMs }
    val laneFreeAt = FloatArray(DANMAKU_LANES) { Float.NEGATIVE_INFINITY }// 每轨空出时刻
    val result = ArrayList<PlacedDanmaku>(sorted.size)
    for (d in sorted) {
        val layout = measurer.measure(
            text = d.content,
            style = TextStyle(
                color = parseDanmakuColor(d.color),
                fontSize = DANMAKU_FONT_SIZE,
                fontWeight = FontWeight.Medium,
                shadow = Shadow(Color.Black.copy(alpha = 0.6f), Offset(0f, 1f), blurRadius = 3f),
            ),
            maxLines = 1,
            softWrap = false,// 禁止换行
        )
        val w = layout.size.width.toFloat()
        val v = (containerW + w) / DANMAKU_DURATION_MS// px/ms
        val enter = d.videoTimeMs.toFloat()
        val tailClearsAt = enter + (w + gapPx) / v// 计算右边越过屏幕右边时间
        val lane = (0 until DANMAKU_LANES).firstOrNull { laneFreeAt[it] <= enter }
            ?: laneFreeAt.indices.minByOrNull { laneFreeAt[it] }!!// 全部占用挑最早空出
        laneFreeAt[lane] = tailClearsAt
        result.add(PlacedDanmaku(d, layout, w, lane))
    }
    return result
}

/// 解析弹幕颜色
fun parseDanmakuColor(hex: String?): Color = try {
    Color((hex ?: "#FFFFFF").toColorInt())
} catch (e: Exception) {
    Color.White
}
