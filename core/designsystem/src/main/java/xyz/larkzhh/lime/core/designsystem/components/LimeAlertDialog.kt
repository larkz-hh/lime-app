package xyz.larkzhh.lime.core.designsystem.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * 通用自定义 AlertDialog 弹窗。
 *
 * @param title 弹窗显示的标题文本
 * @param text 标题下方说明文本（可选）
 * @param onFirstButtonClick 点击左侧按钮时的回调
 * @param onSecondButtonClick 点击右侧按钮时回调
 * @param onDismissRequest 当用户点击弹窗外部区域或按下返回键时触发的回调
 * @param firstButtonText 左侧按钮的文本，默认为 "取消"
 * @param secondButtonText 右侧按钮的文本，默认为 "确定"。
 * @param secondButtonColor 右侧按钮颜色，默认为主色
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LimeAlertDialog(
    title: String,
    onFirstButtonClick: () -> Unit = {},
    onSecondButtonClick: () -> Unit,
    onDismissRequest: () -> Unit,
    text: String? = null,
    firstButtonText: String? = "取消",
    secondButtonText: String = "确定",
    secondButtonColor: Color? = null,
) {
    // null 跟随当前主题主色
    val resolvedSecondColor = secondButtonColor ?: MaterialTheme.colorScheme.primary
    BasicAlertDialog(onDismissRequest = onDismissRequest) {
        LimeAlertDialogContent(
            title = title,
            text = text,
            firstButtonText = firstButtonText,
            secondButtonText = secondButtonText,
            secondButtonColor = resolvedSecondColor,
            onFirstButtonClick = onFirstButtonClick,
            onSecondButtonClick = onSecondButtonClick,
        )
    }
}

// 弹窗内容视图
@Composable
private fun LimeAlertDialogContent(
    title: String,
    text: String?,
    firstButtonText: String?,
    secondButtonText: String,
    secondButtonColor: Color,
    onFirstButtonClick: () -> Unit,
    onSecondButtonClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.widthIn(max = 260.dp),
    ) {
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 20.dp),
            )
            if (text != null) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Start,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 20.dp, bottom = 18.dp),
                )
            }
            HorizontalDivider()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
            ) {
                if (firstButtonText != null) {
                    TextButton(
                        onClick = onFirstButtonClick,
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                    ) {
                        Text(firstButtonText, color = MaterialTheme.colorScheme.onSurface)
                    }
                    VerticalDivider()
                }
                TextButton(
                    onClick = onSecondButtonClick,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                ) {
                    Text(secondButtonText, color = secondButtonColor, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LimeAlertDialogPreview() {
    MaterialTheme {
        LimeAlertDialogContent(
            title = "确认保存笔记至草稿箱吗？",
            text = null,
            firstButtonText = "取消",
            secondButtonText = "确定",
            secondButtonColor = MaterialTheme.colorScheme.primary,
            onFirstButtonClick = {},
            onSecondButtonClick = {},
        )
    }
}
