package xyz.larkzhh.lime.util.text

/// 剥离 Markdown 符号
fun String.stripMarkdown(): String {
    return this
        .replace(Regex("```[\\s\\S]*?```"), " ")
        .replace(Regex("`([^`]*)`"), "$1")
        .replace(Regex("!\\[([^\\]]*)]\\([^)]*\\)"), "$1")
        .replace(Regex("\\[([^\\]]*)]\\([^)]*\\)"), "$1")
        .lineSequence()
        .joinToString(" ") { line ->
            line.trim()
                .replace(Regex("^#{1,6}\\s*"), "")
                .replace(Regex("^\\s*[-*+]\\s+"), "")
                .replace(Regex("^\\s*\\d+\\.\\s+"), "")
                .replace(Regex("^>\\s*"), "")
        }
        .replace(Regex("\\*{1,3}|_{1,3}"), "")
        .replace("|", " ")
        .replace(Regex("\\s+"), " ")
        .trim()
}
