package xyz.larkzhh.lime.ui.detail.translate

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import xyz.larkzhh.lime.R
import xyz.larkzhh.lime.ui.theme.LimeGray
import xyz.larkzhh.lime.ui.theme.LimeLightGray
import xyz.larkzhh.lime.util.TranslateModelInfo
import xyz.larkzhh.lime.util.languageDisplayName

/**
 * 选词翻译的译文弹窗
 *
 * @param state 翻译状态
 * @param onDismiss 关闭面板回调
 * @param onRetry 失败后立即重试回调
 * @param onBackgroundDownload 失败后加入后台下载队列回调
 * @param onSwitchDirection 切换翻译方向回调
 * @param onCopy 复制译文回调
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranslateResultSheet(
    state: TranslateUiState,
    onDismiss: () -> Unit,
    onRetry: () -> Unit,
    onBackgroundDownload: () -> Unit,
    onSwitchDirection: () -> Unit,
    onCopy: (String) -> Unit,
) {
    val context = LocalContext.current
    val packSizeLabel = remember { TranslateModelInfo.downloadSizeLabel(context) }
    val maxSheetHeight = with(LocalDensity.current) {
        (LocalWindowInfo.current.containerSize.height * 0.6f).toDp()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.background,
        scrimColor = Color.Black.copy(alpha = 0.4f),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = maxSheetHeight),
        ) {
            // 标题栏
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 4.dp, top = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.translate_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    // 语言方向标签
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() },
                            ) { onSwitchDirection() }
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = "${languageDisplayName(state.sourceTag)} → ${languageDisplayName(state.targetTag)}",
                            fontSize = 11.sp,
                            color = LimeGray,
                        )
                        Icon(
                            imageVector = Icons.Outlined.SwapHoriz,
                            contentDescription = stringResource(R.string.translate_switch_direction),
                            tint = LimeGray,
                            modifier = Modifier.size(13.dp),
                        )
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = stringResource(R.string.translate_close),
                        tint = LimeGray,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f))

            // 原文、结果
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
            ) {
                Text(
                    text = state.original,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = LimeGray,
                        lineHeight = 20.sp,
                    ),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f), thickness = 0.5.dp)

                when (state.phase) {
                    TranslatePhase.Downloading -> StatusRow(
                        stringResource(R.string.translate_downloading_pack, packSizeLabel)
                    )
                    TranslatePhase.Translating -> StatusRow(stringResource(R.string.translate_translating))
                    TranslatePhase.Error -> ErrorBlock(state.error, onRetry, onBackgroundDownload)
                    TranslatePhase.Done -> ResultBlock(state.result, onCopy)
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

/// 下载、翻译中加载行
@Composable
private fun StatusRow(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(18.dp),
            color = MaterialTheme.colorScheme.primary,
            strokeWidth = 2.dp,
        )
        Text(text = text, fontSize = 13.sp, color = LimeGray)
    }
}

/// 失败提示、重试
@Composable
private fun ErrorBlock(
    message: String?,
    onRetry: () -> Unit,
    onBackgroundDownload: () -> Unit,
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
        Text(text = message ?: stringResource(R.string.translate_error_default), fontSize = 13.sp, color = LimeGray)
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { onBackgroundDownload() }
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = stringResource(R.string.translate_background_download), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primary)
                    .clickable { onRetry() }
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = stringResource(R.string.translate_retry), fontSize = 13.sp, color = Color.White)
            }
        }
    }
}

/// 结果行
@Composable
private fun ResultBlock(result: String, onCopy: (String) -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
        if (result.isBlank()) {
            Text(
                text = stringResource(R.string.translate_result_missing),
                fontSize = 13.sp,
                color = LimeGray,
            )
        } else {
            Text(
                text = result,
                style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface, lineHeight = 22.sp),
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primary)
                        .clickable { onCopy(result) }
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = stringResource(R.string.translate_copy_result), fontSize = 13.sp, color = Color.White)
                }
            }
        }
    }
}
