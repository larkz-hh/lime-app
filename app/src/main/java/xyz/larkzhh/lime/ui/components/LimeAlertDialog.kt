package xyz.larkzhh.lime.ui.components

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import xyz.larkzhh.lime.ui.theme.LimePrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LimeAlertDialog(
    title: String,
    onFirstButtonClick: () -> Unit,
    onSecondButtonClick: () -> Unit,
    onDismissRequest: () -> Unit,
    firstButtonText: String = "取消",
    secondButtonText: String = "确定",
) {
    BasicAlertDialog(onDismissRequest = onDismissRequest) {
        LimeAlertDialogContent(
            title = title,
            firstButtonText = firstButtonText,
            secondButtonText = secondButtonText,
            onFirstButtonClick = onFirstButtonClick,
            onSecondButtonClick = onSecondButtonClick,
        )
    }
}

// 弹窗内容
@Composable
private fun LimeAlertDialogContent(
    title: String,
    firstButtonText: String,
    secondButtonText: String,
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
            HorizontalDivider()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
            ) {
                TextButton(
                    onClick = onFirstButtonClick,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                ) {
                    Text(firstButtonText, color = MaterialTheme.colorScheme.onSurface)
                }
                VerticalDivider()
                TextButton(
                    onClick = onSecondButtonClick,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                ) {
                    Text(secondButtonText, color = LimePrimary, fontWeight = FontWeight.SemiBold)
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
            firstButtonText = "取消",
            secondButtonText = "确定",
            onFirstButtonClick = {},
            onSecondButtonClick = {},
        )
    }
}