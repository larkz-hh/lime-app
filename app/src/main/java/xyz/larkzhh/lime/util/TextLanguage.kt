package xyz.larkzhh.lime.util

/// 判断文本中英
fun detectLanguageTag(text: String): String {
    var latin = 0
    var eastAsian = 0// 东亚文字计数
    text.forEach { ch ->
        when (ch) {
            in '\u3040'..'\u30FF', in '\uAC00'..'\uD7AF', in '\u4E00'..'\u9FFF' -> eastAsian++// 日韩中
            in 'a'..'z', in 'A'..'Z' -> latin++
        }
    }
    // 汉字为主
    return if (eastAsian > 0 && eastAsian * 2 >= latin) {
        "zh"
    } else if (latin > 0) {
        "en"
    } else {
        "zh"
    }
}

/// 语言标签展示名
fun languageDisplayName(tag: String): String = when (tag) {
    "zh" -> "中文"
    "en" -> "英文"
    else -> tag
}
