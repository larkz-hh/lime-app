package xyz.larkzhh.lime.util.text

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/// 中英判定与近似文本判定
class TextLanguageTest {

    @Test
    fun 纯中文判为zh() {
        assertEquals("zh", detectLanguageTag("今天天气很好"))
    }

    @Test
    fun 纯英文判为en() {
        assertEquals("en", detectLanguageTag("hello world"))
    }

    @Test
    fun 汉字不占优时判为en() {
        assertEquals("en", detectLanguageTag("中文 abcdefg"))
    }

    @Test
    fun 汉字与拉丁等量时判为zh() {
        // 判据是 eastAsian * 2 >= latin，取等号算中文
        assertEquals("zh", detectLanguageTag("中文 abcd"))
    }

    @Test
    fun 假名与谚文计入东亚文字() {
        assertEquals("zh", detectLanguageTag("こんにちは"))
        assertEquals("zh", detectLanguageTag("안녕하세요"))
    }

    @Test
    fun 没有字母时默认zh() {
        assertEquals("zh", detectLanguageTag("123 456 ..."))
        assertEquals("zh", detectLanguageTag(""))
    }

    @Test
    fun 语言标签展示名() {
        assertEquals("中文", languageDisplayName("zh"))
        assertEquals("英文", languageDisplayName("en"))
    }

    @Test
    fun 未知标签原样返回() {
        assertEquals("ja", languageDisplayName("ja"))
        assertEquals("", languageDisplayName(""))
    }

    @Test
    fun 完全相同判为近似() {
        assertTrue(isNearlyIdentical("今天天气很好", "今天天气很好"))
    }

    @Test
    fun 差一个字在阈值内() {
        assertTrue(isNearlyIdentical("hello world", "hello worle"))
    }

    @Test
    fun 差四分之一是阈值边界() {
        // maxLen = 8，编辑距离 4，恰好越过 distance * 4 <= maxLen
        assertFalse(isNearlyIdentical("abcdefgh", "abcdXXXX"))
    }

    @Test
    fun 长度差超过一倍直接否决() {
        assertFalse(isNearlyIdentical("ab", "abcdefghij"))
        assertFalse(isNearlyIdentical("abcdefghij", "ab"))
    }

    @Test
    fun 空串或纯空白判为不近似() {
        assertFalse(isNearlyIdentical("", "abc"))
        assertFalse(isNearlyIdentical("abc", ""))
        assertFalse(isNearlyIdentical("   ", "abc"))
    }

    @Test
    fun 超过两千字符直接否决() {
        val long = "a".repeat(2001)
        assertFalse(isNearlyIdentical(long, long))
    }
}
