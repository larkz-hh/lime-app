package xyz.larkzhh.lime.ui.video.components

import androidx.compose.ui.graphics.Color
import androidx.core.graphics.toColorInt

/// 解析弹幕颜色
fun parseDanmakuColor(hex: String?): Color = try {
    Color((hex ?: "#FFFFFF").toColorInt())
} catch (e: Exception) {
    Color.White
}
