package xyz.larkzhh.lime.ui.components

import androidx.compose.runtime.Composable
import xyz.larkzhh.lime.ui.theme.LimePrimary

/// 取消关注确认弹窗
@Composable
fun UnfollowConfirmDialog(
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
) {
    LimeAlertDialog(
        title = "不再关注 TA？",
        text = null,
        firstButtonText = "取消",
        secondButtonText = "不再关注",
        secondButtonColor = LimePrimary,
        onFirstButtonClick = onCancel,
        onSecondButtonClick = onConfirm,
        onDismissRequest = onCancel,
    )
}
