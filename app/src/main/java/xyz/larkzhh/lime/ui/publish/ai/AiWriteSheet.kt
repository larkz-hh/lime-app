package xyz.larkzhh.lime.ui.publish.ai

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import xyz.larkzhh.lime.R
import xyz.larkzhh.lime.ui.theme.LimePrimary
import xyz.larkzhh.lime.ui.theme.LimeWhite
import xyz.larkzhh.lime.util.copyToClipboard
import xyz.larkzhh.lime.util.showToast

/**
 * 发布页 AI 帮写底部弹窗
 *
 * 看图写文案、起标题、润色、续写、精简。
 * 润色、续写、精简正文至少 10 个字，不足时提示。
 *
 * @param content 当前正文
 * @param imageUris 当前已选的本地图片
 * @param onApplyContent 回填正文回调
 * @param onApplyTitle 回填标题回调
 * @param onDismiss 关闭弹窗回调
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiWriteSheet(
    content: String,
    imageUris: List<Uri>,
    onApplyContent: (String) -> Unit,
    onApplyTitle: (String) -> Unit,
    onDismiss: () -> Unit,
    viewModel: AiWriteViewModel,
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // 关闭时断开请求
    fun dismiss() {
        if (state.isGenerating) viewModel.stop()
        onDismiss()
    }

    // 首次打开面板默认开始
    LaunchedEffect(Unit) {
        if (!state.isGenerating && !state.finished && state.text.isEmpty() && state.error == null) {
            viewModel.start(AiWriteAction.defaultFor(imageUris.isNotEmpty()), content, imageUris)
        }
    }

    // 流式输出时自动滚动到底部
    LaunchedEffect(state.text) {
        scrollState.animateScrollTo(scrollState.maxValue)
    }

    // 回填正文
    fun applyContent() {
        val result = state.text
        val final = if (state.action == AiWriteAction.CONTINUE) {
            when {
                content.isBlank() -> result
                content.endsWith("\n") -> content + result
                else -> content + "\n" + result
            }
        } else {
            result
        }
        onApplyContent(final)
        dismiss()
    }

    ModalBottomSheet(
        onDismissRequest = ::dismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .navigationBarsPadding(),
        ) {
            // 标题栏
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painterResource(R.drawable.ic_ai),
                    contentDescription = null,
                    tint = LimePrimary,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "AI 帮写",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = ::dismiss) {
                    Icon(Icons.Outlined.Close, contentDescription = "关闭")
                }
            }

            // 动作栏
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AiWriteAction.visibleFor(imageUris.isNotEmpty()).forEach { action ->
                    val selected = state.action == action
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(18.dp))
                            .background(
                                if (selected) LimePrimary
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
                            )
                            .clickable { viewModel.start(action, content, imageUris) }
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                    ) {
                        Text(
                            text = action.label,
                            color = if (selected) Color.White
                            else MaterialTheme.colorScheme.onSurface,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // 结果区
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp, max = 280.dp)
                    .verticalScroll(scrollState)
                    .padding(vertical = 8.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    when {
                        // 生成中且无内容
                        state.isGenerating && state.text.isEmpty() -> Row(
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = LimePrimary,
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                if (state.isUploading) (state.uploadProgressText ?: "正在上传图片…")
                                else "正在生成…",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }

                        // 起标题完成
                        state.action == AiWriteAction.TITLE &&
                                state.finished && state.titles.isNotEmpty() ->
                            state.titles.forEachIndexed { index, title ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f))
                                        .clickable {
                                            onApplyTitle(title)
                                            dismiss()
                                        }
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = "${index + 1}.",
                                        color = LimePrimary,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Text(
                                        text = title,
                                        style = MaterialTheme.typography.bodyLarge,
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                            }

                        // 有内容
                        state.text.isNotEmpty() -> Text(
                            text = if (state.isGenerating) state.text + "▍" else state.text,
                            style = MaterialTheme.typography.bodyLarge,
                            lineHeight = 24.sp,
                        )
                    }
                    // 出错
                    state.error?.let {
                        Text(
                            text = it,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }

            // 状态说明行
            Text(
                text = when {
                    state.isUploading -> "正在上传图片，可随时停止"
                    state.isGenerating -> "生成中，可随时停止"
                    state.action == AiWriteAction.TITLE && state.finished -> "点选一个标题填入标题栏"
                    state.action == AiWriteAction.CAPTION && state.finished && state.text.isNotEmpty() ->
                        "文案将填入正文"
                    state.action == AiWriteAction.CONTINUE && state.finished && state.text.isNotEmpty() ->
                        "续写内容将追加到正文末尾"
                    state.finished && state.text.isNotEmpty() ->
                        "共 ${state.text.length} 字 · 替换正文将覆盖当前内容"
                    else -> " "
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                style = MaterialTheme.typography.bodySmall,
            )

            Spacer(Modifier.height(12.dp))

            // 底部操作区
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (state.isGenerating) {
                    // 生成中，停止
                    OutlinedButton(
                        onClick = viewModel::stop,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                    ) {
                        Text(
                            if (state.isUploading) "停止" else "停止生成",
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                } else {
                    // 重新生成、复制
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(
                            onClick = { viewModel.start(state.action, content, imageUris) },
                            enabled = content.isNotBlank() || imageUris.isNotEmpty(),
                            modifier = Modifier.size(32.dp),
                        ) {
                            Icon(
                                Icons.Outlined.Refresh,
                                contentDescription = "重新生成",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                        IconButton(
                            onClick = {
                                state.text.copyToClipboard(context)
                                "已复制".showToast(context)
                            },
                            enabled = state.text.isNotEmpty(),
                            modifier = Modifier.size(32.dp),
                        ) {
                            Icon(
                                Icons.Outlined.ContentCopy,
                                contentDescription = "复制",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                    // 回填按钮
                    if (state.action != AiWriteAction.TITLE) {
                        Button(
                            onClick = ::applyContent,
                            enabled = state.finished && state.text.isNotEmpty(),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = LimePrimary),
                        ) {
                            Text(
                                text = when (state.action) {
                                    AiWriteAction.CONTINUE -> "追加到正文"
                                    AiWriteAction.CAPTION -> "填入正文"
                                    else -> "替换正文"
                                },
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
