package xyz.larkzhh.lime.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import xyz.larkzhh.lime.R

/**
 * 强制下线弹窗
 */
@Composable
fun ForceLogoutDialog(onConfirm: () -> Unit) {
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
        ),
    ) {
        ForceLogoutDialogContent(onConfirm = onConfirm)
    }
}

/// 弹窗内容（预览用）
@Composable
internal fun ForceLogoutDialogContent(onConfirm: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.widthIn(max = 280.dp),
    ) {
        Column {
            Text(
                text = stringResource(R.string.force_logout_title),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 20.dp),
            )
            Text(
                text = stringResource(R.string.force_logout_message),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Start,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, bottom = 18.dp),
            )
            HorizontalDivider()
            TextButton(
                onClick = onConfirm,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.CenterHorizontally),
            ) {
                Text(
                    stringResource(R.string.force_logout_login_again),
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 320, heightDp = 260)
@Composable
private fun ForceLogoutDialogPreview() {
    MaterialTheme {
        ForceLogoutDialogContent(onConfirm = {})
    }
}
