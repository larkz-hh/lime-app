package xyz.larkzhh.lime.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import xyz.larkzhh.lime.core.designsystem.R as DesignSystemR
import xyz.larkzhh.lime.core.ui.R

/**
 * 强制下线弹窗
 */
@Composable
fun ForceLogoutDialog(onConfirm: () -> Unit) {
    LimeAlertDialog(
        title = stringResource(DesignSystemR.string.force_logout_title),
        text = stringResource(DesignSystemR.string.force_logout_message),
        firstButtonText = null,
        secondButtonText = stringResource(R.string.force_logout_login_again),
        onFirstButtonClick = onConfirm,
        onSecondButtonClick = onConfirm,
        onDismissRequest = {},
    )
}

@Preview(showBackground = true, widthDp = 320, heightDp = 260)
@Composable
private fun ForceLogoutDialogPreview() {
    MaterialTheme {
        ForceLogoutDialog(onConfirm = {})
    }
}
