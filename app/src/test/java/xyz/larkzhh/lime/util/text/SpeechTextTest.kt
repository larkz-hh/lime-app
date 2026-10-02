package xyz.larkzhh.lime.util.text

import org.junit.Assert.assertEquals
import org.junit.Test

private const val WORD_MS = 200L

/**
 * 语音识别文本整理的单测
 */
class SpeechTextTest {

    /// 造一段词序列
    private fun words(vararg parts: Pair<String, Long>): List<SpeechText.Word> {
        var cursor = 0L
        return parts.map { (text, gap) ->
            val start = cursor + gap
            val end = start + WORD_MS
            cursor = end
            SpeechText.Word(text, start, end)
        }
    }

    @Test
    fun 词间空格被去掉() {
        val result = SpeechText.format(
            words("今天" to 0L, "天气" to 0L, "很" to 0L, "好" to 0L),
            final = true,
        )
        assertEquals("今天天气很好。", result)
    }

    @Test
    fun 强停顿断句弱停顿逗号() {
        assertEquals(
            "你好。世界。",
            SpeechText.format(words("你好" to 0L, "世界" to 900L), final = true),
        )
        assertEquals(
            "你好，世界。",
            SpeechText.format(words("你好" to 0L, "世界" to 400L), final = true),
        )
        assertEquals(
            "你好世界。",
            SpeechText.format(words("你好" to 0L, "世界" to 10L), final = true),
        )
    }

    @Test
    fun 疑问句补问号() {
        assertEquals(
            "今天天气怎么样？",
            SpeechText.format(words("今天" to 0L, "天气" to 0L, "怎么样" to 0L), final = true),
        )
        assertEquals(
            "你吃饭了吗？",
            SpeechText.format(words("你" to 0L, "吃饭" to 0L, "了" to 0L, "吗" to 0L), final = true),
        )
    }

    @Test
    fun 中英混排保留英文空格() {
        assertEquals(
            "打开 iOS 设置。",
            SpeechText.format(words("打开" to 0L, "iOS" to 0L, "设置" to 0L), final = true),
        )
        assertEquals(
            "hello world。",
            SpeechText.format(words("hello" to 0L, "world" to 0L), final = true),
        )
    }

    @Test
    fun 临时尾巴不断句也不补句末标点() {
        assertEquals(
            "你好。世界再见",
            SpeechText.format(words("你好" to 0L, "世界" to 900L), tail = "再见"),
        )
    }

    @Test
    fun 预览阶段不补句末标点() {
        assertEquals("你好", SpeechText.format(words("你好" to 0L)))
    }

    @Test
    fun 连接词前补逗号() {
        assertEquals(
            "我今天很累，但是我还是来了。",
            SpeechText.format(
                words("我" to 0L, "今天" to 0L, "很累" to 0L, "但是" to 0L, "我" to 0L, "还是" to 0L, "来了" to 0L),
                final = true,
            ),
        )
    }

    @Test
    fun 从句太短不补逗号() {
        assertEquals(
            "他但是没来。",
            SpeechText.format(words("他" to 0L, "但是" to 0L, "没来" to 0L), final = true),
        )
    }

    @Test
    fun 没有词级时间戳时按段断句() {
        val words = listOf(SpeechText.untimed("你好"), SpeechText.untimed("世界"))
        assertEquals("你好。世界", SpeechText.format(words))
    }

    @Test
    fun 已带标点不重复补() {
        assertEquals(
            "你好。世界。",
            SpeechText.format(words("你好。" to 0L, "世界" to 900L), final = true),
        )
    }

    @Test
    fun 丢弃unk与空白() {
        val words = listOf(
            SpeechText.untimed("[unk]"),
            SpeechText.untimed("  "),
            SpeechText.untimed("你好"),
        )
        assertEquals("你好。", SpeechText.format(words, final = true))
    }

    @Test
    fun 空输入返回空串() {
        assertEquals("", SpeechText.format(emptyList(), final = true))
        assertEquals("", SpeechText.format(emptyList(), tail = "   "))
    }
}
