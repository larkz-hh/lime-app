package xyz.larkzhh.lime.ui.video.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import xyz.larkzhh.lime.R
import xyz.larkzhh.lime.ui.theme.LimeDark
import xyz.larkzhh.lime.ui.theme.LimeGray
import xyz.larkzhh.lime.ui.theme.LimeLightGray
import xyz.larkzhh.lime.ui.theme.LimePrimary

/// 弹幕颜色选择
private val DANMAKU_COLORS = listOf(
    "#FFFFFF", "#FF3B30", "#FF9500", "#FFCC00",
    "#4CD964", "#34AADC", "#007AFF", "#5856D6", "#FF2D55",
)

/**
 * 弹幕输入面板
 * @param color 当前选择颜色
 * @param onColorChange 颜色选择回调
 * @param onToggleOff 关闭弹幕
 * @param onSend 发送回调
 * @param onDismiss 收起面板
 */
@Composable
fun DanmakuInputSheet(
    color: String,
    onColorChange: (String) -> Unit,
    onToggleOff: () -> Unit,
    onSend: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    var text by remember { mutableStateOf("") }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    BackHandler { onDismiss() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.4f))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
            ) { onDismiss() },
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                ) { },
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
            color = MaterialTheme.colorScheme.background,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // 弹幕开关
                Icon(
                    painter = painterResource(R.drawable.ic_barrage),
                    contentDescription = "关闭弹幕",
                    tint = Color.Black,
                    modifier = Modifier
                        .size(24.dp)
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() },
                            onClick = onToggleOff,
                        ),
                )
                // 颜色板
                ColorPalette(color = color, onColorChange = onColorChange)

                // 输入框
                BasicTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 40.dp, max = 120.dp)
                        .background(LimeLightGray, RoundedCornerShape(20.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                        .focusRequester(focusRequester),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        color = LimeDark,
                        fontSize = 15.sp,
                    ),
                    cursorBrush = SolidColor(LimePrimary),
                    decorationBox = { inner ->
                        if (text.isEmpty()) {
                            Text(text = "发个弹幕呗… (∠・ω< )⌒☆", color = LimeGray, fontSize = 15.sp)
                        }
                        inner()
                    },
                )

                // 发送
                val canSend = text.isNotBlank()
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (canSend) LimePrimary else LimeLightGray)
                        .clickable(enabled = canSend) {
                            onSend(text.trim())
                            text = ""
                        }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "发送",
                        fontSize = 13.sp,
                        color = if (canSend) Color.White else LimeGray,
                    )
                }
            }
        }
    }
}

/// 颜色板
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ColorPalette(
    color: String,
    onColorChange: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(parseDanmakuColor(color))
                .border(1.dp, LimeGray, CircleShape)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                ) { expanded = true },
        )
        if (expanded) {
            Popup(
                onDismissRequest = { expanded = false },
                properties = PopupProperties(focusable = false),
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.background,
                    shadowElevation = 8.dp,
                    modifier = Modifier
                        .padding(bottom = 8.dp)
                        .width(180.dp)
                        .wrapContentHeight(),
                ) {
                    FlowRow(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        DANMAKU_COLORS.forEach { hex ->
                            val selected = hex.equals(color, ignoreCase = true)
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(parseDanmakuColor(hex))
                                    .border(
                                        width = if (selected) 2.dp else 1.dp,
                                        color = if (selected) LimePrimary else LimeGray.copy(alpha = 0.5f),
                                        shape = CircleShape,
                                    )
                                    .clickable(
                                        indication = null,
                                        interactionSource = remember { MutableInteractionSource() },
                                    ) {
                                        onColorChange(hex)
                                        expanded = false
                                    },
                            )
                        }
                    }
                }
            }
        }
    }
}
