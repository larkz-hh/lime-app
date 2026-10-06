package xyz.larkzhh.lime.ui.detail.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import xyz.larkzhh.lime.core.designsystem.R as DesignSystemR
import xyz.larkzhh.lime.data.local.SpeechPackStatus
import xyz.larkzhh.lime.ui.theme.LimeGray

/// 语音转文字预览框
@Composable
internal fun TranscribePreview(
    text: String,
    status: SpeechPackStatus,
    progress: Int,
    onDownload: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .heightIn(min = 88.dp, max = 104.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        when {
            status == SpeechPackStatus.Downloading -> Column(
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(DesignSystemR.string.voice_downloading_pack, progress),
                    fontSize = 13.sp,
                    color = LimeGray,
                )
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { progress / 100f },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            status != SpeechPackStatus.Downloaded -> Column(
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(DesignSystemR.string.voice_pack_missing),
                    fontSize = 13.sp,
                    color = LimeGray,
                )
                TextButton(onClick = onDownload) {
                    Text(text = stringResource(DesignSystemR.string.voice_pack_download), fontSize = 13.sp)
                }
            }

            text.isBlank() -> Text(
                text = stringResource(DesignSystemR.string.voice_transcribing),
                fontSize = 13.sp,
                color = LimeGray,
            )

            else -> Text(
                text = text,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 20.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
            )
        }
    }
}
