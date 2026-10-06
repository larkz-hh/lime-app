package xyz.larkzhh.lime.util.text

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/// 时间格式化与同日判定
class TimeFormatTest {

    private val minute = 60_000L
    private val day = 86_400_000L

    @Test
    fun 无法解析的时间原样返回() {
        assertEquals("not-a-time", formatRelativeTime("not-a-time"))
        assertEquals("", formatRelativeTime(""))
    }

    @Test
    fun 带毫秒的ISO时间能解析() {
        val iso = LocalDateTime.now()
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")) + ".123"
        assertEquals("刚刚", formatRelativeTime(iso))
    }

    @Test
    fun 一分钟内为刚刚() {
        val now = System.currentTimeMillis()
        assertEquals("刚刚", formatRelativeTime(now))
        assertEquals("刚刚", formatRelativeTime(now - 30_000L))
    }

    @Test
    fun 一小时内按分钟显示() {
        val now = System.currentTimeMillis()
        assertEquals("5分钟前", formatRelativeTime(now - 5 * minute))
        assertEquals("59分钟前", formatRelativeTime(now - 59 * minute))
    }

    @Test
    fun 两到六天前按天显示() {
        val now = System.currentTimeMillis()
        assertEquals("3天前", formatRelativeTime(now - 3 * day))
        assertEquals("6天前", formatRelativeTime(now - 6 * day))
    }

    @Test
    fun 会话时间非正数返回空串() {
        assertEquals("", formatConversationTime(0))
        assertEquals("", formatConversationTime(-1))
    }

    @Test
    fun 会话时间跨年显示完整年月日() {
        val lastYear = Instant.parse("2020-01-02T03:04:05Z").epochSecond
        assertEquals("2020年1月2日", formatConversationTime(lastYear))
    }

    @Test
    fun 同一秒属于同一天() {
        val now = Instant.now().epochSecond
        assertTrue(isSameChatDay(now, now))
    }

    @Test
    fun 相隔一天不算同一天() {
        val now = Instant.now().epochSecond
        assertFalse(isSameChatDay(now, now - 24 * 3600))
        assertFalse(isSameChatDay(now - 24 * 3600, now))
    }
}
