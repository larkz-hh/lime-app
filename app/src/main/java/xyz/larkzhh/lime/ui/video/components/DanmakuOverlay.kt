package xyz.larkzhh.lime.ui.video.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
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

/**
 * 顶部弹幕区
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
    val lanes = remember(danmakuList) { assignLanes(danmakuList) }
    val widths = remember { mutableStateMapOf<Long, Int>() }// 每条弹幕测量宽度px

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
        val laneHeightPx = with(LocalDensity.current) { LANE_HEIGHT.toPx() }

        lanes.forEach { (d, lane) ->
            val frozen = pausedDanmakuId == d.id
            val clockMs = if (frozen) frozenMs else nowMs
            val progress = (clockMs - d.videoTimeMs).toFloat() / DANMAKU_DURATION_MS
            if (!frozen && progress !in 0f..1f) return@forEach// 不在窗口内

            val w = widths[d.id] ?: 0
            val x = containerW - progress * (containerW + w)
            val y = lane * laneHeightPx

            Text(
                text = d.content,
                color = parseDanmakuColor(d.color),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                modifier = Modifier
                    .zIndex(if (frozen) 1f else 0f)
                    .graphicsLayer {
                        translationX = x
                        translationY = y
                    }
                    .onSizeChanged { widths[d.id] = it.width }
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                    ) {
                        onDanmakuClick(d.id, nowMs)
                    },
            )
        }

        // 气泡菜单
        val paused = pausedDanmakuId?.let { id -> lanes.firstOrNull { it.first.id == id } }
        if (paused != null) {
            val (d, lane) = paused
            val w = widths[d.id] ?: 0
            val progress = (frozenMs - d.videoTimeMs).toFloat() / DANMAKU_DURATION_MS
            val danmakuX = containerW - progress * (containerW + w)
            val centerX = danmakuX + w / 2f
            val laneTopY = lane * laneHeightPx

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

/// 解析弹幕颜色
fun parseDanmakuColor(hex: String?): Color = try {
    Color((hex ?: "#FFFFFF").toColorInt())
} catch (e: Exception) {
    Color.White
}

/// 分配弹幕轨道
private fun assignLanes(list: List<DanmakuData>): List<Pair<DanmakuData, Int>> {
    val sorted = list.sortedBy { it.videoTimeMs }
    return sorted.mapIndexed { index, d -> d to (index % DANMAKU_LANES) }
}