package xyz.larkzhh.lime.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class SheetAction(
    val label: String,
    val textColor: Color = Color(0xFF1C1C1E),
    val fontWeight: FontWeight = FontWeight.Normal,
    val onClick: () -> Unit,
)

/**
 * 通用底部操作菜单组件
 *
 * @param visible 控制菜单的显示与隐藏状态
 * @param onDismiss 关闭回调
 * @param actions 菜单项列表
 * @param modifier 外部传入的 Modifier
 * @param backgroundColor 背景颜色，默认为白色。
 */
@Composable
fun BottomActionSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    actions: List<SheetAction>,
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color.White,
) {
    Box(modifier = modifier.fillMaxSize()) {
        // 蒙层，淡入淡出，点击关闭
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f))
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                        onClick = onDismiss,
                    ),
            )
        }

        // 操作卡片，从底部滑入、滑出
        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    //.navigationBarsPadding()
                    //.padding(horizontal = 16.dp)
                    //.padding(bottom = 16.dp),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(backgroundColor)
                        // 消费触摸，防止穿透到蒙层
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() },
                            onClick = {},
                        ),
                ) {
                    actions.forEachIndexed { index, action ->
                        if (index > 0) {
                            HorizontalDivider(color = Color(0xFFE5E5E5), thickness = 0.5.dp)
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(
                                    indication = null,
                                    interactionSource = remember { MutableInteractionSource() },
                                ) {
                                    onDismiss()
                                    action.onClick()
                                }
                                .padding(vertical = 16.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = action.label,
                                color = action.textColor,
                                fontSize = 17.sp,
                                fontWeight = action.fontWeight,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            }
        }
    }
}
