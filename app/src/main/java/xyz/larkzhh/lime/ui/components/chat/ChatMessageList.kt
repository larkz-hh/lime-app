package xyz.larkzhh.lime.ui.components.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import xyz.larkzhh.lime.ui.theme.LimeGray
import xyz.larkzhh.lime.ui.theme.LimeWhite
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private const val timeDividerThresholdMs = 5 * 60 * 1000L

/**
 * 通用消息列表
 */
@Composable
fun ChatMessageList(
    messages: List<ChatBubbleData>,
    modifier: Modifier = Modifier,
    typing: Boolean = false,
    contentPadding: PaddingValues = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
    onRetry: (ChatBubbleData) -> Unit = {},
    onCopy: (ChatBubbleData) -> Unit = {},
    onRegenerate: (ChatBubbleData) -> Unit = {},
    onSpeak: (ChatBubbleData) -> Unit = {},
    onDelete: (ChatBubbleData) -> Unit = {},
    onSelectText: (ChatBubbleData) -> Unit = {},
    onImageClick: (index: Int, images: List<String>) -> Unit = { _, _ -> },
) {
    val listState = rememberLazyListState()
    // 是否在底部
    val stickToBottom by remember {
        derivedStateOf { listState.firstVisibleItemIndex <= 1 && listState.firstVisibleItemScrollOffset < 60 }
    }

    // 新消息、流式增长滚动跟随
    val lastContent = messages.lastOrNull()?.content.orEmpty()
    LaunchedEffect(messages.size, lastContent.length) {
        if (stickToBottom) {
            listState.scrollToItem(0)
        }
    }

    val reversed = remember(messages, typing) { messages.reversed() }

    // 会话最后一条消息
    val lastMessageId = messages.lastOrNull()?.id

    val scope = rememberCoroutineScope()

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            reverseLayout = true,
            contentPadding = contentPadding,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
        if (typing) {
            item(key = "typing") {
                Row(modifier = Modifier.fillMaxWidth()) {
                    BoxTyping()
                }
            }
        }
        itemsIndexed(reversed, key = { _, it -> it.id }) { index, data ->
            Column {
                val next = reversed.getOrNull(index + 1)
                val showDivider = next != null &&
                        data.timestamp != null && next.timestamp != null &&
                        kotlin.math.abs(data.timestamp - next.timestamp) > timeDividerThresholdMs
                // 时间分隔线
                if (showDivider) {
                    TimeDivider(data.timestamp)
                }
                ChatMessageBubble(
                    data = data,
                    onRetry = if (data.status == ChatBubbleStatus.FAILED && data.isSelf) {
                        { onRetry(data) }
                    } else null,
                    onCopy = if (!data.isSelf && data.content.isNotBlank()) {
                        { onCopy(data) }
                    } else null,
                    onRegenerate = if (!data.isSelf && data.status == ChatBubbleStatus.DONE &&
                        data.id == lastMessageId
                    ) {
                        { onRegenerate(data) }
                    } else null,
                    onSpeak = if (!data.isSelf && data.status == ChatBubbleStatus.DONE && data.content.isNotBlank()) {
                        { onSpeak(data) }
                    } else null,
                    onDelete = { onDelete(data) },
                    onSelectText = if (data.content.isNotBlank()) {
                        { onSelectText(data) }
                    } else null,
                    onImageClick = onImageClick,
                )
            }
        }
        }

        // 滚动回底部按钮
        AnimatedVisibility(
            visible = !stickToBottom,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 12.dp, bottom = 8.dp),
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .shadow(6.dp, CircleShape, spotColor = Color.Black.copy(alpha = 0.12f))
                    .clip(CircleShape)
                    .background(LimeWhite)
                    .clickable { scope.launch { listState.animateScrollToItem(0) } },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.ArrowDownward,
                    contentDescription = "回到底部",
                    tint = Color(0xFF3A3A3A),
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

@Composable
private fun BoxTyping() {
    Column(
        modifier = Modifier
            .clip(
                RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = 4.dp,
                    bottomEnd = 16.dp,
                )
            )
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        TypingIndicator()
    }
}

@Composable
private fun TimeDivider(timestamp: Long?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
    ) {
        Text(
            text = formatChatDividerTime(timestamp),
            color = LimeGray,
            fontSize = 11.sp,
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f))
                .padding(horizontal = 10.dp, vertical = 3.dp),
        )
    }
}

private fun formatChatDividerTime(timestamp: Long?): String {
    if (timestamp == null) return ""
    val time = Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault())
    val now = Instant.now().atZone(ZoneId.systemDefault())
    val hm = time.format(DateTimeFormatter.ofPattern("HH:mm"))
    return when {
        time.toLocalDate() == now.toLocalDate() -> hm
        time.year == now.year -> time.format(DateTimeFormatter.ofPattern("M月d日 HH:mm"))
        else -> time.format(DateTimeFormatter.ofPattern("yyyy年M月d日 HH:mm"))
    }
}
