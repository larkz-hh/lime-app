package xyz.larkzhh.lime.util.text

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

/// 判断两段文本是否几乎相同
fun isNearlyIdentical(a: String, b: String): Boolean {
    if (a.isBlank() || b.isBlank()) return false
    val maxLen = maxOf(a.length, b.length)
    if (maxLen > 2000) return false
    // 长度差过大
    if (a.length * 2 < b.length || b.length * 2 < a.length) return false
    // 滚动两行 Levenshtein 编辑距离
    var prev = IntArray(b.length + 1) { it }
    var curr = IntArray(b.length + 1)
    for (i in 1..a.length) {
        curr[0] = i
        for (j in 1..b.length) {
            val cost = if (a[i - 1] == b[j - 1]) 0 else 1
            curr[j] = minOf(prev[j] + 1, curr[j - 1] + 1, prev[j - 1] + cost)
        }
        val swap = prev
        prev = curr
        curr = swap
    }
    val distance = prev[b.length]
    return distance * 4 <= maxLen
}
