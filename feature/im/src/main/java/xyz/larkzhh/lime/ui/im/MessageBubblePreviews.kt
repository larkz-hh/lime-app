package xyz.larkzhh.lime.ui.im

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import xyz.larkzhh.lime.domain.model.ImMessage
import xyz.larkzhh.lime.ui.theme.DarkChatBubbleColors
import xyz.larkzhh.lime.ui.theme.LightChatBubbleColors
import xyz.larkzhh.lime.ui.theme.LocalChatBubbleColors

/// 浅色 IM 聊天气泡预览
@Preview(showBackground = true, name = "IM 气泡 · 浅色", widthDp = 400)
@Composable
private fun ImBubblePreviewLight() {
    ImBubblePreviewFrame(dark = false)
}

/// 深色 IM 聊天气泡预览
@Preview(showBackground = true, name = "IM 气泡 · 深色", widthDp = 400)
@Composable
private fun ImBubblePreviewDark() {
    ImBubblePreviewFrame(dark = true)
}

@Composable
private fun ImBubblePreviewFrame(dark: Boolean) {
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
        MaterialTheme(colorScheme = colorScheme) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(12.dp),
            ) {
                // 对方
                MessageBubble(
                    message = ImMessage(
                        id = "1",
                        senderId = "u2",
                        isSelf = false,
                        timestamp = 0L,
                        text = "晚上好，这条是对方发来的消息",
                    ),
                    avatar = null,
                    onAvatarClick = {},
                    onCopy = {},
                    onRevoke = {},
                    onDelete = {},
                    onImageClick = {},
                )
                // 自己
                MessageBubble(
                    message = ImMessage(
                        id = "2",
                        senderId = "u1",
                        isSelf = true,
                        timestamp = 0L,
                        text = "你好！这条是我发的消息",
                    ),
                    avatar = null,
                    onAvatarClick = {},
                    onCopy = {},
                    onRevoke = {},
                    onDelete = {},
                    onImageClick = {},
                )
            }
        }
    }
}
