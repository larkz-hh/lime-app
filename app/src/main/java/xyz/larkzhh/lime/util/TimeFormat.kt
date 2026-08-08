package xyz.larkzhh.lime.util

import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

private val isoFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")

fun formatRelativeTime(isoTime: String): String {
    val time = try {
        LocalDateTime.parse(isoTime.substringBefore("."), isoFormatter)
            .atOffset(ZoneOffset.UTC)
            .atZoneSameInstant(ZoneId.systemDefault())// 转换为当前系统所在设备的时区
            .toLocalDateTime()
    } catch (_: Exception) {
        return isoTime
    }
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
