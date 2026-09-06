package xyz.larkzhh.lime.ui.components.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.EmojiEmotions
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import xyz.larkzhh.lime.R
import xyz.larkzhh.lime.domain.model.ChatNote
import xyz.larkzhh.lime.ui.theme.LimeGray
import xyz.larkzhh.lime.ui.theme.LimeLightGray
import xyz.larkzhh.lime.ui.theme.LimeWhite

private val InputPillColor = LimeWhite
private val SendColor = Color(0xFF111111)
private val SendDisabledColor = Color(0xFFBBBBBB)

/**
 * 通用聊天输入栏
 */
@Composable
fun ChatInputBar(
    text: String,
    onTextChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
    images: List<ChatInputImage> = emptyList(),
    note: ChatNote? = null,
    sending: Boolean = false,
    canSend: Boolean = true,
    placeholder: String = "",
    onAddClick: () -> Unit = {},
    onRemoveImage: (uri: String) -> Unit = {},
    onRetryImage: (uri: String) -> Unit = {},
    onRemoveNote: () -> Unit = {},
    onSend: () -> Unit = {},
    onStop: () -> Unit = {},
    showEmojiToggle: Boolean = false,
    emojiActive: Boolean = false,
    onEmojiClick: () -> Unit = {},
) {
    val effectivePlaceholder = placeholder.ifBlank { stringResource(R.string.chat_ask_hint) }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
    ) {
        // 已选引用笔记
        if (note != null) {
            NotePreviewChip(
                note = note,
                onRemove = onRemoveNote,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
        }
        // 已选图片缩略图
        if (images.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                images.forEach { image ->
                    PendingImageThumb(
                        image = image,
                        onRemove = { onRemoveImage(image.uri) },
                        onRetry = { onRetryImage(image.uri) },
                    )
                }
            }
        }

        // 底部输入框
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .shadow(6.dp, RoundedCornerShape(28.dp), spotColor = Color.Black.copy(alpha = 0.12f))
                .clip(RoundedCornerShape(28.dp))
                .background(InputPillColor)
                .border(1.dp, Color.Black.copy(alpha = 0.05f), RoundedCornerShape(28.dp))
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 左侧加号
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(LimeLightGray)
                    .clickable(onClick = onAddClick),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Outlined.Add,
                    contentDescription = stringResource(R.string.chat_add),
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(24.dp),
                )
            }
            Spacer(Modifier.width(10.dp))

            // 输入框
            Box(modifier = Modifier.weight(1f)) {
                BasicTextField(
                    value = text,
                    onValueChange = onTextChange,
                    modifier = Modifier
                        .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
                        .fillMaxWidth()
                        .heightIn(min = 40.dp, max = 120.dp)
                        .padding(vertical = 8.dp),
                    textStyle = TextStyle(
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 16.sp,
                        lineHeight = 22.sp,
                    ),
                    maxLines = 6,
                    decorationBox = { innerTextField ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.weight(1f)) {
                                if (text.isEmpty()) {
                                    Text(
                                        text = effectivePlaceholder,
                                        color = LimeGray,
                                        fontSize = 16.sp,
                                    )
                                }
                                innerTextField()
                            }
                            if (showEmojiToggle) {
                                IconButton(
                                    onClick = onEmojiClick,
                                    modifier = Modifier.size(36.dp),
                                ) {
                                    Icon(
                                        imageVector = if (emojiActive) Icons.Outlined.Keyboard else Icons.Outlined.EmojiEmotions,
                                        contentDescription = if (emojiActive) {
                                            stringResource(R.string.chat_keyboard)
                                        } else {
                                            stringResource(R.string.chat_emoji)
                                        },
                                        tint = if (emojiActive) MaterialTheme.colorScheme.primary else LimeGray,
                                        modifier = Modifier.size(24.dp),
                                    )
                                }
                            }
                        }
                    },
                )
            }
            Spacer(Modifier.width(8.dp))

            // 发送、停止
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            sending -> SendColor
                            canSend -> SendColor
                            else -> SendDisabledColor
                        }
                    )
                    .clickable(enabled = sending || canSend) {
                        if (sending) onStop() else onSend()
                    },
                contentAlignment = Alignment.Center,
            ) {
                if (sending) {
                    Icon(
                        Icons.Filled.Stop,
                        contentDescription = stringResource(R.string.chat_stop_generating),
                        tint = LimeWhite,
                        modifier = Modifier.size(18.dp),
                    )
                } else {
                    Icon(
                        Icons.Filled.ArrowUpward,
                        contentDescription = stringResource(R.string.chat_send),
                        tint = LimeWhite,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun PendingImageThumb(
    image: ChatInputImage,
    onRemove: () -> Unit,
    onRetry: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(64.dp)
            .clip(RoundedCornerShape(10.dp))
            .then(
                if (image.state == ChatInputImageState.FAILED) {
                    Modifier.border(1.5.dp, MaterialTheme.colorScheme.error, RoundedCornerShape(10.dp))
                } else Modifier
            )
            .clickable(enabled = image.state == ChatInputImageState.FAILED, onClick = onRetry),
    ) {
        AsyncImage(
            model = image.uri,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(64.dp),
        )
        if (image.state == ChatInputImageState.UPLOADING) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(Color.Black.copy(alpha = 0.35f)),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = LimeWhite,
                )
            }
        }
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(18.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.55f))
                .clickable(onClick = onRemove),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Outlined.Close,
                contentDescription = stringResource(R.string.chat_remove_image),
                tint = LimeWhite,
                modifier = Modifier.size(12.dp),
            )
        }
    }
}

/// 已选引用笔记预览条
@Composable
private fun NotePreviewChip(
    note: ChatNote,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(LimeLightGray)
            .padding(start = 6.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = note.cover,
            contentDescription = note.title,
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(6.dp)),
            contentScale = ContentScale.Crop,
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = note.title?.ifBlank { stringResource(R.string.chat_referenced_note) } ?: stringResource(R.string.chat_referenced_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onRemove, modifier = Modifier.size(22.dp)) {
            Icon(
                Icons.Outlined.Close,
                contentDescription = stringResource(R.string.chat_remove_note),
                tint = LimeGray,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}
