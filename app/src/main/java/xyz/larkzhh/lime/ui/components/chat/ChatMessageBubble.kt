package xyz.larkzhh.lime.ui.components.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Error
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import kotlin.math.roundToInt
import xyz.larkzhh.lime.R
import xyz.larkzhh.lime.domain.model.ChatNote
import xyz.larkzhh.lime.ui.theme.LimeGray
import xyz.larkzhh.lime.ui.theme.LocalChatBubbleColors
import xyz.larkzhh.lime.ui.theme.DarkChatBubbleColors
import xyz.larkzhh.lime.ui.theme.LightChatBubbleColors
import xyz.larkzhh.lime.util.TtsManager

/// 用户气泡最大宽度
private const val USER_BUBBLE_MAX_WIDTH = 320

/**
 * 通用聊天气泡
 */
@Composable
fun ChatMessageBubble(
    data: ChatBubbleData,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null,
    onCopy: (() -> Unit)? = null,
    onRegenerate: (() -> Unit)? = null,
    onSpeak: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    onSelectText: (() -> Unit)? = null,
    onImageClick: (index: Int, images: List<String>) -> Unit = { _, _ -> },
) {
    val isSelf = data.isSelf
    var showMenu by remember { mutableStateOf(false) }
    var longPressOffset by remember { mutableStateOf(Offset.Zero) }

    Box(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = {},
                        onLongPress = { offset ->
                            longPressOffset = offset
                            showMenu = true
                        },
                    )
                },
            horizontalAlignment = if (isSelf) Alignment.End else Alignment.Start,
        ) {
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // 发送中、失败
                if (isSelf && data.status == ChatBubbleStatus.FAILED && onRetry != null) {
                    IconButton(onClick = onRetry, modifier = Modifier.size(30.dp)) {
                        Icon(
                            Icons.Outlined.Error,
                            contentDescription = stringResource(R.string.chat_send_failed_retry),
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                } else if (isSelf && data.status == ChatBubbleStatus.SENDING) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .padding(bottom = 6.dp)
                            .size(14.dp),
                        strokeWidth = 1.5.dp,
                        color = LimeGray,
                    )
                }

                if (isSelf) {
                    SelfBubble(data, onImageClick)
                } else {
                    AiBubble(data, onCopy, onRegenerate, onSpeak, onImageClick)
                }
            }

            // 时间、状态说明
            Row(
                modifier = Modifier.padding(top = 3.dp, start = 4.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (!data.timeText.isNullOrBlank()) {
                    Text(text = data.timeText, color = LimeGray, fontSize = 10.sp)
                }
                if (data.status == ChatBubbleStatus.STOPPED) {
                    if (!data.timeText.isNullOrBlank()) Spacer(Modifier.width(6.dp))
                    Text(text = stringResource(R.string.chat_stopped), color = LimeGray, fontSize = 10.sp)
                }
            }
        }

        // 长按菜单
        Box(
            modifier = Modifier.offset {
                IntOffset(longPressOffset.x.roundToInt(), longPressOffset.y.roundToInt())
            }
        ) {
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false },
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                shape = RoundedCornerShape(12.dp),
            ) {
                val itemModifier = Modifier.width(150.dp).height(56.dp)
                val itemStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp)
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.chat_copy), style = itemStyle) },
                    onClick = { showMenu = false; onCopy?.invoke() },
                    enabled = onCopy != null,
                    modifier = itemModifier,
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.chat_select_text), style = itemStyle) },
                    onClick = { showMenu = false; onSelectText?.invoke() },
                    enabled = onSelectText != null,
                    modifier = itemModifier,
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error, style = itemStyle) },
                    onClick = { showMenu = false; onDelete?.invoke() },
                    enabled = onDelete != null,
                    modifier = itemModifier,
                )
            }
        }
    }
}

/// 用户消息
@Composable
private fun SelfBubble(
    data: ChatBubbleData,
    onImageClick: (index: Int, images: List<String>) -> Unit,
) {
    Column(horizontalAlignment = Alignment.End) {
        // 引用笔记卡片
        if (data.note != null) {
            NoteCardChip(data.note, modifier = Modifier.widthIn(max = 200.dp))
            Spacer(Modifier.size(8.dp))
        }
        // 图片横滑行
        if (data.images.isNotEmpty()) {
            HorizontalImageRow(images = data.images, onImageClick = onImageClick)
            Spacer(Modifier.size(8.dp))
        }
        if (data.content.isNotBlank()) {
            // 气泡颜色来自主题（LocalChatBubbleColors），深浅色无需页面判断
            val bubbleColors = LocalChatBubbleColors.current
            Text(
                text = data.content,
                color = bubbleColors.grayBubbleContent,
                style = MaterialTheme.typography.bodyLarge,
                lineHeight = 22.sp,
                modifier = Modifier
                    .widthIn(max = USER_BUBBLE_MAX_WIDTH.dp)
                    .clip(
                        RoundedCornerShape(
                            topStart = 18.dp,
                            topEnd = 18.dp,
                            bottomStart = 18.dp,
                            bottomEnd = 4.dp,
                        )
                    )
                    .background(bubbleColors.grayBubble)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
            )
        }
    }
}

/// AI 气泡
@Composable
private fun AiBubble(
    data: ChatBubbleData,
    onCopy: (() -> Unit)?,
    onRegenerate: (() -> Unit)?,
    onSpeak: (() -> Unit)?,
    onImageClick: (index: Int, images: List<String>) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        if (data.content.isNotBlank()) {
            when {
                // Markdown：流式、结束分块渲染
                data.renderMarkdown ->
                    StreamingMarkdown(
                        content = data.content,
                        renderTailAsMarkdown = data.status != ChatBubbleStatus.STREAMING,
                    )

                else ->
                    Text(
                        text = data.content,
                        color = MaterialTheme.colorScheme.onBackground,
                        style = MaterialTheme.typography.bodyLarge,
                        lineHeight = 22.sp,
                    )
            }
        }
        if (data.images.isNotEmpty() && data.renderMarkdown.not()) {
            Spacer(Modifier.size(6.dp))
            ImageMessageGrid(images = data.images, onImageClick = onImageClick)
        }
        // 操作行
        if (data.status == ChatBubbleStatus.DONE && data.content.isNotBlank()) {
            // 正在朗读 id
            val speakingThis = TtsManager.speakingMessageId.collectAsState().value == data.id
            Row(
                modifier = Modifier.padding(top = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                if (onCopy != null) {
                    BubbleActionIcon(Icons.Outlined.ContentCopy, stringResource(R.string.chat_copy), onCopy)
                }
                if (onRegenerate != null) {
                    BubbleActionIcon(Icons.Outlined.Refresh, stringResource(R.string.chat_regenerate), onRegenerate)
                }
                if (onSpeak != null) {
                    BubbleActionIcon(
                        icon = if (speakingThis) Icons.Filled.Pause else Icons.AutoMirrored.Outlined.VolumeUp,
                        description = if (speakingThis) stringResource(R.string.chat_stop_speak) else stringResource(R.string.chat_speak),
                        iconSize = 18.dp,
                        onClick = { if (speakingThis) TtsManager.stop() else onSpeak() },
                    )
                }
            }
        }
    }
}

/// 气泡底部小图标
@Composable
private fun BubbleActionIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    onClick: () -> Unit,
    iconSize: androidx.compose.ui.unit.Dp = 16.dp,
) {
    IconButton(onClick = onClick, modifier = Modifier.size(28.dp)) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = LimeGray,
            modifier = Modifier.size(iconSize),
        )
    }
}

/// 流式 Markdown 渲染
@Composable
private fun StreamingMarkdown(
    content: String,
    renderTailAsMarkdown: Boolean = false,
) {
    val split = remember(content) { splitStreamingMarkdown(content) }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // 渲染已完成的块
        split.completedBlocks.forEachIndexed { index, block ->
            key(index) {
                MarkdownMessageContent(content = block)
            }
        }
        if (split.tail.isNotBlank()) {
            if (renderTailAsMarkdown) {
                // 消息结束，Markdown 渲染最后一段
                MarkdownMessageContent(content = split.tail)
            } else {
                // 流式未完成段落，纯文本追加
                Text(
                    text = split.tail,
                    color = MaterialTheme.colorScheme.onBackground,
                    style = MaterialTheme.typography.bodyLarge,
                    lineHeight = 22.sp,
                )
            }
        }
    }
}

/// 引用笔记卡片
@Composable
private fun NoteCardChip(
    note: ChatNote,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, Color.Black.copy(alpha = 0.06f), RoundedCornerShape(10.dp))
            .padding(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (note.cover != null) {
            AsyncImage(
                model = note.cover,
                contentDescription = note.title,
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(6.dp)),
                contentScale = ContentScale.Crop,
            )
            Spacer(Modifier.width(8.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = note.title?.ifBlank { stringResource(R.string.chat_quote_note) } ?: stringResource(R.string.chat_quote_note),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = stringResource(R.string.chat_quote_note),
                color = LimeGray,
                fontSize = 10.sp,
            )
        }
    }
}


/// 浅色 AI 聊天页气泡预览
@Preview(showBackground = true, name = "AI 聊天气泡 · 浅色", widthDp = 400)
@Composable
private fun AiChatBubblePreviewLight() {
    ChatBubblePreviewTheme(dark = false) {
        ChatBubbleSamples()
    }
}

/// 深色 AI 聊天页气泡预览
@Preview(showBackground = true, name = "AI 聊天气泡 · 深色", widthDp = 400)
@Composable
private fun AiChatBubblePreviewDark() {
    ChatBubblePreviewTheme(dark = true) {
        ChatBubbleSamples()
    }
}

@Composable
private fun ChatBubbleSamples() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // 我的消息
        ChatMessageBubble(
            data = ChatBubbleData(
                id = 1,
                isSelf = true,
                content = "你好，帮我看看这套气泡配色行不行？",
                status = ChatBubbleStatus.DONE,
            ),
        )
        // AI 回复
        ChatMessageBubble(
            data = ChatBubbleData(
                id = 2,
                isSelf = false,
                content = "**可以**，这套方案挺稳的。\n\n- 我的气泡浅色浅灰、深色深灰\n- 深色模式下文字自动切白\n- IM 自己的气泡保持品牌蓝",
                status = ChatBubbleStatus.DONE,
                renderMarkdown = true,
            ),
            onCopy = {},
            onRegenerate = {},
            onSpeak = {},
        )
    }
}

/// 预览主题
@Composable
private fun ChatBubblePreviewTheme(
    dark: Boolean,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (dark) {
        darkColorScheme(
            background = Color(0xFF000000),
            surfaceVariant = Color(0xFF1B1B1B),
        )
    } else {
        lightColorScheme(
            background = Color(0xFFF5F5F5),
            surfaceVariant = Color(0xFFF1F1F1),
        )
    }
    CompositionLocalProvider(
        LocalChatBubbleColors provides if (dark) DarkChatBubbleColors else LightChatBubbleColors,
    ) {
        MaterialTheme(colorScheme = colorScheme, content = content)
    }
}
