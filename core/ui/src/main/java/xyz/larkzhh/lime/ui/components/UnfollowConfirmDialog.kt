package xyz.larkzhh.lime.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import xyz.larkzhh.lime.core.designsystem.R as DesignSystemR
import xyz.larkzhh.lime.core.ui.R

/// 取消关注确认弹窗
@Composable
fun UnfollowConfirmDialog(
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
) {
    LimeAlertDialog(
        title = stringResource(R.string.unfollow_title),
        text = null,
        firstButtonText = stringResource(DesignSystemR.string.cancel),
        secondButtonText = stringResource(R.string.unfollow_confirm),
        secondButtonColor = MaterialTheme.colorScheme.primary,
        onFirstButtonClick = onCancel,
        onSecondButtonClick = onConfirm,
        onDismissRequest = onCancel,
    )
}
