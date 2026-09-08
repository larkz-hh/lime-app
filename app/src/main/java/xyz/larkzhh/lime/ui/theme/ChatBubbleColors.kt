package xyz.larkzhh.lime.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * 聊天气泡颜色
 */
data class ChatBubbleColors(
    val grayBubble: Color,
    val grayBubbleContent: Color,
    val blueBubble: Color,
    val blueBubbleContent: Color,
)

/// 浅色主题气泡色
val LightChatBubbleColors = ChatBubbleColors(
    grayBubble = Color(0xFFE8E8E8),
    grayBubbleContent = Color(0xFF111111),
    blueBubble = Color(0xFF3D5AFE),
    blueBubbleContent = Color.White,
)

/// 深色主题气泡色
val DarkChatBubbleColors = ChatBubbleColors(
    grayBubble = Color(0xFF2E2E2E),
    grayBubbleContent = Color.White,
    blueBubble = Color(0xFF3D5AFE),
    blueBubbleContent = Color.White,
)

/// 当前主题气泡色
val LocalChatBubbleColors = staticCompositionLocalOf { LightChatBubbleColors }
