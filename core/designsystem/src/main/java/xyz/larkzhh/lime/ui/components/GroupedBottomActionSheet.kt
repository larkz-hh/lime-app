package xyz.larkzhh.lime.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import xyz.larkzhh.swipeback.blockPageSwipe

data class GroupedSheetAction(
    val label: String,
    val icon: ImageVector,
    val textColor: Color = Color(0xFF1C1C1E),
    val fontWeight: FontWeight = FontWeight.Normal,
    val iconSize: Dp = 24.dp,
    val fontSize: TextUnit = 17.sp,
    val onClick: () -> Unit,
)

/**
 * 分组底部操作菜单外壳
 *
 * @param visible 菜单的显示与隐藏状态
 * @param onDismiss 关闭回调
 * @param modifier 外部传入的 Modifier
 * @param sheetColor 抽屉背景色
 * @param content 抽屉内容
 */
@Composable
fun GroupedBottomActionSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    sheetColor: Color = Color(0xFFF2F2F7),
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(modifier = modifier.fillMaxSize().blockPageSwipe(visible)) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(animationSpec = tween(200)),
            exit = fadeOut(animationSpec = tween(200)),
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

        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically(
                animationSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing),
                initialOffsetY = { it },
            ) + fadeIn(animationSpec = tween(200)),
            exit = slideOutVertically(
                animationSpec = tween(durationMillis = 300, easing = FastOutLinearInEasing),
                targetOffsetY = { it },
            ) + fadeOut(animationSpec = tween(180)),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                    .background(sheetColor)
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp)
                    .padding(top = 16.dp, bottom = 20.dp)
                    // 消费触摸，防止穿透到蒙层
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                        onClick = {},
                    ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                content = content,
            )
        }
    }
}

/**
 * 分组底部操作菜单
 *
 * @param visible 抽屉是否可见
 * @param onDismiss 菜单关闭回调
 * @param groups 操作分组列表
 * @param modifier 修饰符
 * @param sheetColor 抽屉背景色
 * @param cardColor 操作卡片分组背景色
 */
@Composable
fun GroupedBottomActionSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    groups: List<List<GroupedSheetAction>>,
    modifier: Modifier = Modifier,
    sheetColor: Color = Color(0xFFF2F2F7),
    cardColor: Color = Color.White,
) {
    GroupedBottomActionSheet(
        visible = visible,
        onDismiss = onDismiss,
        modifier = modifier,
        sheetColor = sheetColor,
    ) {
        groups.forEach { actions ->
            SheetGroup(cardColor = cardColor) {
                actions.forEachIndexed { index, action ->
                    if (index > 0) SheetRowDivider()
                    SheetActionRow(action = action, onDismiss = onDismiss)
                }
            }
        }
    }
}

/// 分组卡片容器
@Composable
fun SheetGroup(
    modifier: Modifier = Modifier,
    cardColor: Color = MaterialTheme.colorScheme.surface,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(cardColor),
        content = content,
    )
}

/// 分隔线
@Composable
fun SheetRowDivider(startIndent: Dp = 56.dp) {
    HorizontalDivider(
        modifier = Modifier.padding(start = startIndent),
        color = Color(0xFFE5E5E5),
        thickness = 0.5.dp,
    )
}

/// 操作行
@Composable
fun SheetActionRow(
    action: GroupedSheetAction,
    onDismiss: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
            ) {
                onDismiss()
                action.onClick()
            }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = action.icon,
            contentDescription = action.label,
            tint = action.textColor,
            modifier = Modifier.size(action.iconSize),
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = action.label,
            color = action.textColor,
            fontSize = action.fontSize,
            fontWeight = action.fontWeight,
        )
    }
}
