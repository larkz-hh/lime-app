package xyz.larkzhh.lime.ui.components.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.outlined.Error
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import xyz.larkzhh.lime.ui.theme.LimeGray

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
    onImageClick: (index: Int, images: List<String>) -> Unit = { _, _ -> },
) {
    val isSelf = data.isSelf

    Column(
        modifier = modifier.fillMaxWidth(),
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
                SelfBubble(data, onCopy, onImageClick)
            } else {
                AiBubble(data, onCopy, onImageClick)
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
}

/// 用户消息
@Composable
private fun SelfBubble(
    data: ChatBubbleData,
    onCopy: (() -> Unit)?,
    onImageClick: (index: Int, images: List<String>) -> Unit,
) {
    Column(horizontalAlignment = Alignment.End) {
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
                    .let { base ->
                        if (onCopy != null) {
                            base.combinedClickable(onClick = {}, onLongClick = { onCopy() })
                        } else base
                    }
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
    onImageClick: (index: Int, images: List<String>) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .let { base ->
                if (onCopy != null) base.combinedClickable(onClick = {}, onLongClick = { onCopy() })
                else base
            },
    ) {
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
