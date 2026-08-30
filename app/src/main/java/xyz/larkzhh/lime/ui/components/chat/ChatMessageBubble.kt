package xyz.larkzhh.lime.ui.components.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import xyz.larkzhh.lime.domain.model.ChatNote
import xyz.larkzhh.lime.ui.theme.LimeGray
import xyz.larkzhh.lime.util.TtsManager

/// 用户气泡色
private val UserBubbleColor = Color(0xFFF1F1F1)
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

    Box(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(onClick = {}, onLongClick = { showMenu = true }),
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
                            contentDescription = "发送失败，点击重发",
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
                    Text(text = "已停止", color = LimeGray, fontSize = 10.sp)
                }
            }
        }

        // 长按菜单
        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            DropdownMenuItem(
                text = { Text("复制") },
                onClick = { showMenu = false; onCopy?.invoke() },
                enabled = onCopy != null,
            )
            DropdownMenuItem(
                text = { Text("选取文字") },
                onClick = { showMenu = false; onSelectText?.invoke() },
                enabled = onSelectText != null,
            )
            DropdownMenuItem(
                text = { Text("删除", color = MaterialTheme.colorScheme.error) },
                onClick = { showMenu = false; onDelete?.invoke() },
                enabled = onDelete != null,
            )
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
            Text(
                text = data.content,
                color = MaterialTheme.colorScheme.onBackground,
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
                    .background(UserBubbleColor)
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
                // 流式 Markdown
                data.renderMarkdown && data.status == ChatBubbleStatus.STREAMING ->
                    StreamingMarkdown(data.content)
                // 完整 Markdown
                data.renderMarkdown ->
                    MarkdownMessageContent(content = data.content)

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
                    BubbleActionIcon(Icons.Outlined.ContentCopy, "复制", onCopy)
                }
                if (onRegenerate != null) {
                    BubbleActionIcon(Icons.Outlined.Refresh, "重新生成", onRegenerate)
                }
                if (onSpeak != null) {
                    BubbleActionIcon(
                        icon = if (speakingThis) Icons.Filled.Pause else Icons.AutoMirrored.Outlined.VolumeUp,
                        description = if (speakingThis) "停止朗读" else "朗读",
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
private fun StreamingMarkdown(content: String) {
    val split = remember(content) { splitStreamingMarkdown(content) }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // 渲染已完成的块
        split.completedBlocks.forEachIndexed { index, block ->
            key(index) {
                MarkdownMessageContent(content = block)
            }
        }
        if (split.tail.isNotBlank()) {
            Text(
                text = split.tail,
                color = MaterialTheme.colorScheme.onBackground,
                style = MaterialTheme.typography.bodyLarge,
                lineHeight = 22.sp,
            )
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
                text = note.title?.ifBlank { "引用笔记" } ?: "引用笔记",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "引用笔记",
                color = LimeGray,
                fontSize = 10.sp,
            )
        }
    }
}
