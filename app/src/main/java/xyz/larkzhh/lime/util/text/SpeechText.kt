package xyz.larkzhh.lime.util.text

/// 没无词级时间信息时的占位值
private const val UNKNOWN_MS = -1L

/// Vosk 识别不出来的词
private const val UNK_TOKEN = "[unk]"

/**
 * 语音识别文本整理
 */
object SpeechText {
    data class Word(val text: String, val startMs: Long, val endMs: Long) {
        val timed: Boolean get() = startMs in 0..endMs
    }

    /// 没有时间信息的词
    fun untimed(text: String): Word = Word(text, UNKNOWN_MS, UNKNOWN_MS)

    /// 弱停顿补逗号
    private const val PAUSE_COMMA_MS = 260L

    /// 强停顿断句
    private const val PAUSE_SENTENCE_MS = 700L

    /// 盘古之白
    private const val SPACE_BETWEEN_CJK_AND_LATIN = true

    /// 连接词前补逗号的最小前置字符数
    private const val MIN_CLAUSE_BEFORE_MARKER = 3

    /// 连接、转折词补逗号
    private val CLAUSE_MARKERS = listOf(
        "但是", "不过", "然而", "可是", "因为", "所以", "因此", "于是",
        "如果", "要是", "虽然", "尽管", "即使", "然后", "接着", "此外",
        "首先", "其次",
    )

    /// 疑问句
    private val QUESTION_TAILS = setOf('吗', '呢', '么', '嘛')

    /// 疑问句
    private val QUESTION_WORDS = listOf(
        "什么", "怎么", "为什么", "为啥", "哪", "谁", "多少", "几点", "几号", "多久",
        "是不是", "有没有", "能不能", "可不可以", "行不行", "好不好", "对不对",
    )

    /// 标点
    private const val PUNCTUATION = "，。！？；：、,.!?;:…—～~"

    /**
     * 把词序列整理成连贯的中文
     *
     * @param words 已确认词
     * @param tail 临时文本
     * @param final 是否收尾
     */
    fun format(words: List<Word>, tail: String = "", final: Boolean = false): String {
        val tokens = buildList(words.size + 1) {
            for (word in words) {
                val text = word.text.trim()
                if (text.isEmpty() || text == UNK_TOKEN) continue
                add(word.copy(text = text))
            }
            if (tail.isNotBlank()) add(untimed(tail.trim()))
        }
        if (tokens.isEmpty()) return ""

        val builder = StringBuilder()
        var sentenceStart = 0
        val hasTail = tail.isNotBlank()

        for (index in tokens.indices) {
            if (index > 0) {
                val isTail = hasTail && index == tokens.lastIndex
                val clauseLength = builder.length - sentenceStart
                when (decideBreak(tokens[index - 1], tokens[index], isTail, clauseLength)) {
                    Break.Comma -> if (!builder.endsWithPunctuation()) builder.append('，')
                    Break.Sentence -> {
                        if (!builder.endsWithPunctuation()) {
                            builder.append(if (isQuestion(builder, sentenceStart)) '？' else '。')
                        }
                        sentenceStart = builder.length
                    }

                    Break.None -> Unit
                }
            }
            appendToken(builder, tokens[index].text)
        }

        if (final && !builder.endsWithPunctuation()) {
            builder.append(if (isQuestion(builder, sentenceStart)) '？' else '。')
        }
        return builder.toString()
    }

    /// 断词
    private fun decideBreak(prev: Word, current: Word, isTail: Boolean, clauseLength: Int): Break {
        if (isTail) return Break.None
        if (!prev.timed || !current.timed) return Break.Sentence
        val gap = current.startMs - prev.endMs
        if (gap >= PAUSE_SENTENCE_MS) return Break.Sentence
        if (gap >= PAUSE_COMMA_MS) return Break.Comma
        if (clauseLength >= MIN_CLAUSE_BEFORE_MARKER && startsMarker(current.text)) return Break.Comma
        return Break.None
    }

   /// 追加词
    private fun appendToken(builder: StringBuilder, text: String) {
        var separated = builder.isNotEmpty()
        for (char in text) {
            if (char.isWhitespace()) {
                separated = true
                continue
            }
            if (separated && needsSpace(builder.lastOrNull(), char)) builder.append(' ')
            builder.append(char)
            separated = false
        }
    }

    /// 需要空格
    private fun needsSpace(prev: Char?, next: Char): Boolean {
        if (prev == null) return false
        val prevAscii = isAsciiWord(prev)
        val nextAscii = isAsciiWord(next)
        if (prevAscii && nextAscii) return true
        if (!SPACE_BETWEEN_CJK_AND_LATIN) return false
        return (isHan(prev) && nextAscii) || (prevAscii && isHan(next))
    }

    /// 是否为 ASCII 字母或数字
    private fun isAsciiWord(char: Char): Boolean = char.code < 128 && char.isLetterOrDigit()

    /// 是否为汉字
    private fun isHan(char: Char): Boolean = when (char.code) {
        in 0x3400..0x4DBF, in 0x4E00..0x9FFF, in 0xF900..0xFAFF -> true
        else -> false
    }

    /// 是否为疑问句
    private fun isQuestion(builder: StringBuilder, from: Int): Boolean {
        if (from >= builder.length) return false
        val clause = builder.substring(from)
        if (clause.isEmpty()) return false
        if (clause.last() in QUESTION_TAILS) return true
        return QUESTION_WORDS.any { clause.contains(it) }
    }

    /// 是否以连接词开头
    private fun startsMarker(text: String): Boolean = CLAUSE_MARKERS.any { text.startsWith(it) }

    /// 末尾是否有标点
    private fun StringBuilder.endsWithPunctuation(): Boolean {
        val last = lastOrNull() ?: return false
        return last in PUNCTUATION
    }

    /// 断句类型
    private enum class Break { None, Comma, Sentence }
}
