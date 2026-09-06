package xyz.larkzhh.lime.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import xyz.larkzhh.lime.R
import xyz.larkzhh.lime.ui.theme.LimePrimary

/// 取消关注确认弹窗
@Composable
fun UnfollowConfirmDialog(
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
) {
    LimeAlertDialog(
        title = stringResource(R.string.unfollow_title),
        text = null,
        firstButtonText = stringResource(R.string.cancel),
        secondButtonText = stringResource(R.string.unfollow_confirm),
        secondButtonColor = LimePrimary,
        onFirstButtonClick = onCancel,
        onSecondButtonClick = onConfirm,
        onDismissRequest = onCancel,
    )
}
