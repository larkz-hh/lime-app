package xyz.larkzhh.lime.core.designsystem.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * 开关
 *
 * @param checked 当前开关状态
 * @param onCheckedChange 点击切换回调
 * @param enabled 是否可用
 * @param checkedTrackColor 开启时轨道颜色
 * @param uncheckedTrackColor 关闭时轨道颜色
 * @param thumbColor thumb 颜色
 */
@Composable
fun LimeSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    checkedTrackColor: Color = MaterialTheme.colorScheme.primary,
    uncheckedTrackColor: Color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f),
    thumbColor: Color = Color.White,
) {
    val trackWidth = 42.dp
    val trackHeight = 26.dp
    val thumbInset = 2.dp
    val thumbSize = trackHeight - thumbInset * 2

    val trackColor by animateColorAsState(
        targetValue = if (checked) checkedTrackColor else uncheckedTrackColor,
        animationSpec = tween(durationMillis = 200),
    )
    val thumbFraction by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = tween(durationMillis = 200),
    )
    // 可滑动的总距离
    val travel = trackWidth - thumbSize - thumbInset * 2
    val thumbX = thumbInset + travel * thumbFraction

    Box(
        modifier = modifier
            .alpha(if (enabled) 1f else 0.4f)
            .size(width = trackWidth, height = trackHeight)
            .clip(RoundedCornerShape(trackHeight / 2))
            .background(trackColor)
            .then(
                onCheckedChange?.let { callback ->
                    Modifier.clickable(
                        enabled = enabled,
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                        onClick = { callback(!checked) },
                    )
                } ?: Modifier
            ),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset(x = thumbX)
                .size(thumbSize)
                .shadow(elevation = 1.dp, shape = CircleShape, clip = false)
                .clip(CircleShape)
                .background(thumbColor),
        )
    }
}
