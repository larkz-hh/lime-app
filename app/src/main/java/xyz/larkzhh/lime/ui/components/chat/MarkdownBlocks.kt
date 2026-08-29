package xyz.larkzhh.lime.ui.components.chat

/// 流式切块结果
internal data class MarkdownSplit(
    val completedBlocks: List<String>,// 已闭合
    val tail: String,// 未完成
)

/// 块类型
private enum class BlockKind { PARAGRAPH, HEADING, LIST, QUOTE, TABLE, CODE }

/**
 * 流式 Markdown 文本切分器
 * - 已完成块，不再重解析；
 * - 尾块，纯文本平滑追加
 */
internal fun splitStreamingMarkdown(text: String): MarkdownSplit {
    if (text.isBlank()) return MarkdownSplit(emptyList(), "")

    val completed = mutableListOf<String>()
    val current = StringBuilder()
    var kind: BlockKind? = null
    var fenceMarker: String? = null

    /// 刷出当前块
    fun flush() {
        val block = current.toString().trimEnd()
        if (block.isNotBlank()) completed.add(block)
        current.setLength(0)
    }

    /// 行级分类
    fun classify(t: String): BlockKind = when {
        t.startsWith("```") || t.startsWith("~~~") -> BlockKind.CODE
        t.matches(Regex("^#{1,6}\\s.*")) -> BlockKind.HEADING
        t.matches(Regex("^\\s*([-*+]|\\d+\\.)\\s+.*")) -> BlockKind.LIST
        t.startsWith(">") -> BlockKind.QUOTE
        t.contains("|") -> BlockKind.TABLE
        else -> BlockKind.PARAGRAPH
    }

    for (raw in text.split('\n')) {
        val t = raw.trim()

        // 闭合围栏
        if (fenceMarker != null) {
            current.appendLine(raw)
            if (t.startsWith(fenceMarker) && t.length >= fenceMarker.length && t.all { it == fenceMarker[0] }) {
                flush()
                kind = null
                fenceMarker = null
            }
            continue
        }

        // 开口围栏
        if (t.startsWith("```") || t.startsWith("~~~")) {
            flush()
            current.appendLine(raw)
            fenceMarker = if (t.startsWith("```")) "```" else "~~~"
            kind = BlockKind.CODE
            continue
        }

        // 空行结束当前块
        if (t.isEmpty()) {
            flush()
            kind = null
            continue
        }

        // 块类型变化
        val k = classify(t)
        if (kind != null && kind != k) {
            flush()
        }
        current.appendLine(raw)
        kind = k
    }

    val tail = current.toString().trimEnd()
    return MarkdownSplit(completedBlocks = completed, tail = tail)
}
