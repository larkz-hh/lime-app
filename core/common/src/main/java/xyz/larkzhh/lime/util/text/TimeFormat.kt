package xyz.larkzhh.lime.util.text

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

private val isoFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")

fun formatRelativeTime(isoTime: String): String {
    val time = try {
        LocalDateTime.parse(isoTime.substringBefore("."), isoFormatter)
//            .atOffset(ZoneOffset.UTC)
//            .atZoneSameInstant(ZoneId.systemDefault())// 转换为当前系统所在设备的时区
//            .toLocalDateTime()
    } catch (_: Exception) {
        return isoTime
    }
    return formatRelativeTime(time)
}

fun formatRelativeTime(epochMillis: Long): String {
    val time = Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).toLocalDateTime()
    return formatRelativeTime(time)
}

private fun formatRelativeTime(time: LocalDateTime): String {
    val now = LocalDateTime.now()
    val minutes = ChronoUnit.MINUTES.between(time, now)
    val hours = ChronoUnit.HOURS.between(time, now)
    val days = ChronoUnit.DAYS.between(time.toLocalDate(), now.toLocalDate())
    return when {
        minutes < 1 -> "刚刚"
        minutes < 60 -> "${minutes}分钟前"
        hours < 24 && days == 0L -> "${hours}小时前"
        days == 1L -> "昨天 ${time.hour.toString().padStart(2, '0')}:${time.minute.toString().padStart(2, '0')}"
        days in 2..6 -> "${days}天前"
        time.year == now.year -> "${time.monthValue}月${time.dayOfMonth}日"
        else -> "${time.year}年${time.monthValue}月${time.dayOfMonth}日"
    }
}

/**
 * 会话列表最后消息时间
 */
fun formatConversationTime(timestampSec: Long): String {
    if (timestampSec <= 0) return ""
    val zone = ZoneId.systemDefault()
    val instant = Instant.ofEpochSecond(timestampSec).atZone(zone)
    val now = ZonedDateTime.now()
    return when {
        instant.toLocalDate() == now.toLocalDate() ->
            instant.format(DateTimeFormatter.ofPattern("HH:mm"))

        instant.year == now.year ->
            instant.format(DateTimeFormatter.ofPattern("M月d日"))

        else ->
            instant.format(DateTimeFormatter.ofPattern("yyyy年M月d日"))
    }
}

/// 聊天页时间分组行
fun formatChatTime(timestampSec: Long): String {
    val zone = ZoneId.systemDefault()
    val instant = Instant.ofEpochSecond(timestampSec).atZone(zone)
    val now = ZonedDateTime.now()
    val time = instant.format(DateTimeFormatter.ofPattern("HH:mm"))
    return if (instant.toLocalDate() == now.toLocalDate()) {
        time
    } else if (instant.year == now.year) {
        instant.format(DateTimeFormatter.ofPattern("M月d日 HH:mm"))
    } else {
        instant.format(DateTimeFormatter.ofPattern("yyyy年M月d日 HH:mm"))
    }
}

/// 两条消息是否属于同一天
fun isSameChatDay(aSec: Long, bSec: Long): Boolean {
    val a = Instant.ofEpochSecond(aSec).atZone(ZoneId.systemDefault()).toLocalDate()
    val b = Instant.ofEpochSecond(bSec).atZone(ZoneId.systemDefault()).toLocalDate()
    return a == b
}
