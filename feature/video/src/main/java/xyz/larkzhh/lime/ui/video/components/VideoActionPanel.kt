package xyz.larkzhh.lime.ui.video.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Loop
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import xyz.larkzhh.lime.feature.video.R
import xyz.larkzhh.lime.ui.components.GroupedBottomActionSheet
import xyz.larkzhh.lime.ui.components.GroupedSheetAction
import xyz.larkzhh.lime.ui.components.LimeSwitch
import xyz.larkzhh.lime.ui.components.SheetActionRow
import xyz.larkzhh.lime.ui.components.SheetGroup
import xyz.larkzhh.lime.ui.components.SheetRowDivider
import xyz.larkzhh.lime.ui.theme.LimeGray

/// 倍速选项
val SPEED_OPTIONS = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)

/// 倍速显示
fun formatSpeed(speed: Float): String {
    return "${formatSpeedNumber(speed)}X"
}

/// 倍速显示
fun formatSpeedNumber(speed: Float): String {
    return if (speed % 1f == 0f) "${speed.toInt()}.0" else speed.toString()
}

/**
 * 竖屏长按弹出的操作面板
 *
 * - 保存到相册
 * - 倍速、清屏播放、自动连播
 * - 弹幕开关、弹幕不透明度
 */
@Composable
fun VideoActionPanel(
    visible: Boolean,
    onDismiss: () -> Unit,
    currentSpeed: Float,
    onSpeedChange: (Float) -> Unit,
    danmakuEnabled: Boolean,
    onToggleDanmaku: () -> Unit,
    danmakuOpacity: Float,
    onOpacityChange: (Float) -> Unit,
    autoPlayNext: Boolean,
    onToggleAutoPlayNext: () -> Unit,
    backgroundAudio: Boolean,
    onToggleBackgroundAudio: () -> Unit,
    clearScreen: Boolean,
    onClearScreen: () -> Unit,
    onSaveVideo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    GroupedBottomActionSheet(
        visible = visible,
        onDismiss = onDismiss,
        modifier = modifier,
    ) {
        // 保存到相册
        SheetGroup {
            SheetActionRow(
                action = GroupedSheetAction(
                    label = stringResource(R.string.video_save_to_album),
                    icon = Icons.Outlined.FileDownload,
                    iconSize = 20.dp,
                    fontSize = 15.sp,
                    onClick = onSaveVideo,
                ),
                onDismiss = onDismiss,
            )
        }

        // 倍速、清屏播放、自动连播
        SheetGroup {
            SpeedRow(currentSpeed = currentSpeed, onSpeedChange = onSpeedChange)
            SheetRowDivider(startIndent = 52.dp)
            SwitchRow(
                painterRes = R.drawable.ic_clear_screen,
                label = stringResource(R.string.video_clear_screen_playback),
                checked = clearScreen,
                onToggle = onClearScreen,
            )
            SheetRowDivider(startIndent = 52.dp)
            SwitchRow(
                icon = Icons.Filled.Loop,
                label = stringResource(R.string.video_auto_play_next),
                checked = autoPlayNext,
                onToggle = onToggleAutoPlayNext,
            )
            SheetRowDivider(startIndent = 52.dp)
            SwitchRow(
                icon = Icons.Filled.Headphones,
                label = stringResource(R.string.video_background_playback),
                checked = backgroundAudio,
                onToggle = onToggleBackgroundAudio,
            )
        }

        // 弹幕开关、弹幕不透明度
        SheetGroup {
            SwitchRow(
                painterRes = R.drawable.ic_barrage,
                label = stringResource(R.string.video_danmaku),
                checked = danmakuEnabled,
                onToggle = onToggleDanmaku,
            )
            SheetRowDivider(startIndent = 52.dp)
            OpacityRow(opacity = danmakuOpacity, onOpacityChange = onOpacityChange)
        }
    }
}

/// 开关行
@Composable
private fun SwitchRow(
    label: String,
    checked: Boolean,
    onToggle: () -> Unit,
    icon: ImageVector? = null,
    painterRes: Int? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onToggle,
            )
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when {
            icon != null -> Icon(
                imageVector = icon,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(20.dp),
            )
            painterRes != null -> Icon(
                painter = painterResource(painterRes),
                contentDescription = label,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(Modifier.width(14.dp))
        Text(text = label, color = MaterialTheme.colorScheme.onSurface, fontSize = 15.sp)
        Spacer(Modifier.weight(1f))
        LimeSwitch(
            checked = checked,
            onCheckedChange = null,
        )
    }
}

/// 倍速行
@Composable
private fun SpeedRow(currentSpeed: Float, onSpeedChange: (Float) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_speed),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(14.dp))
        Text(text = stringResource(R.string.video_speed), color = MaterialTheme.colorScheme.onSurface, fontSize = 15.sp)
        Spacer(Modifier.width(12.dp))
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SPEED_OPTIONS.forEach { speed ->
                val selected = speed == currentSpeed
                Text(
                    text = formatSpeedNumber(speed),
                    color = if (selected) MaterialTheme.colorScheme.primary else Color(0xFF8E8E93),
                    fontSize = 13.sp,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    modifier = Modifier.clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                        onClick = { onSpeedChange(speed) },
                    ),
                )
            }
        }
    }
}

/// 弹幕不透明度行
@Composable
private fun OpacityRow(opacity: Float, onOpacityChange: (Float) -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = stringResource(R.string.video_danmaku_opacity), color = MaterialTheme.colorScheme.onSurface, fontSize = 15.sp)
            Spacer(Modifier.weight(1f))
            Text(text = "${(opacity * 100).toInt()}%", color = Color(0xFF8E8E93), fontSize = 14.sp)
        }
        OpacitySlider(
            value = opacity,
            onValueChange = onOpacityChange,
            valueRange = 0.2f..1f,
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp),
        )
    }
}

/// 弹幕不透明度滑条
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OpacitySlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier,
) {
    val min = valueRange.start
    val max = valueRange.endInclusive
    val fraction = if (max > min) ((value - min) / (max - min)).coerceIn(0f, 1f) else 0f
    Slider(
        value = fraction,
        onValueChange = { onValueChange((min + it * (max - min)).coerceIn(min, max)) },
        valueRange = 0f..1f,
        modifier = modifier,
        thumb = {
            Box(
                modifier = Modifier.height(28.dp),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .shadow(elevation = 2.dp, shape = CircleShape, clip = false)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface),
                )
            }
        },
        track = { state ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .clip(RoundedCornerShape(1.dp))
                        .background(Color(0xFFEFEFF0)),
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth(state.value.coerceIn(0f, 1f))
                        .height(2.dp)
                        .clip(RoundedCornerShape(1.dp))
                        .background(MaterialTheme.colorScheme.primary),
                )
            }
        },
    )
}

/**
 * 横屏倍速板
 */
@Composable
fun SpeedDrawer(
    visible: Boolean,
    currentSpeed: Float,
    onSpeedChange: (Float) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        // 蒙层
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(animationSpec = tween(180)),
            exit = fadeOut(animationSpec = tween(180)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f))
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                        onClick = onDismiss,
                    ),
            )
        }

        AnimatedVisibility(
            visible = visible,
            enter = slideInHorizontally(
                animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
                initialOffsetX = { it },
            ) + fadeIn(animationSpec = tween(160)),
            exit = slideOutHorizontally(
                animationSpec = tween(durationMillis = 220, easing = FastOutLinearInEasing),
                targetOffsetX = { it },
            ) + fadeOut(animationSpec = tween(140)),
            modifier = Modifier.align(Alignment.CenterEnd),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(1f / 3f)
                    .background(Color.Black.copy(alpha = 0.55f))
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                        onClick = {},
                    )
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                SPEED_OPTIONS.sortedDescending().forEach { speed ->
                    val selected = speed == currentSpeed
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(LimeGray.copy(alpha = 0.3f))
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() },
                                onClick = { onSpeedChange(speed) },
                            )
                            .padding(vertical = 12.dp, horizontal = 16.dp),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        Text(
                            text = formatSpeed(speed),
                            color = if (selected) MaterialTheme.colorScheme.primary else Color.White,
                            fontSize = 15.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        )
                    }
                }
            }
        }
    }
}
