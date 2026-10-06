package xyz.larkzhh.lime.ui.video.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.exoplayer.ExoPlayer
import xyz.larkzhh.danmaku.DanmakuBubble
import xyz.larkzhh.danmaku.DanmakuBubbleItem
import xyz.larkzhh.danmaku.DanmakuBubbleStyle
import xyz.larkzhh.danmaku.DanmakuClock
import xyz.larkzhh.danmaku.DanmakuItem
import xyz.larkzhh.danmaku.DanmakuOverlay
import xyz.larkzhh.danmaku.DanmakuSelection
import xyz.larkzhh.danmaku.DanmakuStyle
import xyz.larkzhh.lime.R
import xyz.larkzhh.lime.data.network.model.DanmakuData
import xyz.larkzhh.lime.util.copyToClipboard
import xyz.larkzhh.lime.util.showToast

/// 气泡外观
private val LimeBubbleStyle = DanmakuBubbleStyle(
    background = Color(0xFF2C2C2C),
    contentColor = Color.White,
    itemTextStyle = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Normal),
)

/// 重算弹幕窗口的进度步长
private const val WINDOW_STEP_MS = 5_000L

/// 窗口向前保留的时长
private const val WINDOW_LEAD_MS = 30_000L

/**
 * 顶部弹幕区
 *
 * @param danmakuList 当前视频的全部弹幕，播放位置附近的一段
 * @param player 播放器，为 null 时时钟恒为 0，弹幕不入画
 * @param enabled 弹幕开关
 * @param currentUserId 当前登录用户
 * @param noteAuthorId 视频作者
 * @param selection 被冻结的弹幕
 * @param onSelectionChange 选中变化
 * @param onDismissBubble 关闭气泡
 * @param onDelete 删除回调
 * @param opacity 弹幕不透明度
 */
@Composable
fun DanmakuHost(
    danmakuList: List<DanmakuData>,
    player: ExoPlayer?,
    enabled: Boolean,
    currentUserId: Long?,
    noteAuthorId: Long?,
    selection: DanmakuSelection?,
    onSelectionChange: (DanmakuSelection?) -> Unit,
    onDismissBubble: () -> Unit,
    onDelete: (DanmakuData) -> Unit,
    modifier: Modifier = Modifier,
    opacity: Float = 1f,
) {
    val context = LocalContext.current
    val copiedToast = stringResource(R.string.copied)

    val clock = remember(player) { DanmakuClock { player?.currentPosition ?: 0L } }

    // 窗口锚点
    var windowAnchorMs by remember(danmakuList) { mutableLongStateOf(0L) }
    LaunchedEffect(clock, enabled) {
        if (!enabled) return@LaunchedEffect
        while (true) {
            withFrameNanos { }
            val position = clock.positionMs()
            if (position < windowAnchorMs || position - windowAnchorMs > WINDOW_STEP_MS) {
                windowAnchorMs = position
            }
        }
    }

    val trailingMs = DanmakuStyle.Default.durationMillis + WINDOW_STEP_MS
    val items = remember(danmakuList, currentUserId, windowAnchorMs) {
        val from = windowAnchorMs - trailingMs
        val to = windowAnchorMs + WINDOW_LEAD_MS
        danmakuList
            .filter { it.videoTimeMs in from..to }
            .map { it.toDanmakuItem(currentUserId) }
    }

    DanmakuOverlay(
        items = items,
        clock = clock,
        modifier = modifier,
        enabled = enabled,
        opacity = opacity,
        selection = selection,
        onSelectionChange = onSelectionChange,
        selectionContent = { resolved ->
            val data = danmakuList.firstOrNull { it.id == resolved.item.id } ?: return@DanmakuOverlay
            val canDelete = currentUserId != null &&
                (currentUserId == data.author.id || currentUserId == noteAuthorId)
            DanmakuBubble(selection = resolved, style = LimeBubbleStyle) {
                DanmakuBubbleItem(
                    text = stringResource(R.string.chat_copy),
                    leadingIcon = { BubbleIcon(Icons.Outlined.ContentCopy) },
                ) {
                    data.content.copyToClipboard(context)
                    copiedToast.showToast(context)
                    onDismissBubble()
                }
                if (canDelete) {
                    DanmakuBubbleItem(
                        text = stringResource(R.string.delete),
                        leadingIcon = { BubbleIcon(Icons.Outlined.Delete) },
                    ) {
                        onDelete(data)
                        onDismissBubble()
                    }
                }
            }
        },
    )
}

@Composable
private fun BubbleIcon(icon: ImageVector) {
    Icon(
        imageVector = icon,
        contentDescription = null,
        tint = LimeBubbleStyle.contentColor,
        modifier = Modifier.size(16.dp),
    )
}

/// 业务模型映射
private fun DanmakuData.toDanmakuItem(currentUserId: Long?): DanmakuItem = DanmakuItem(
    id = id,
    text = content,
    timeMs = videoTimeMs,
    color = parseDanmakuColor(color),
    isSelf = currentUserId != null && currentUserId == author.id,
)
