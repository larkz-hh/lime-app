package xyz.larkzhh.lime.data.local.notification

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * 站内通知本地缓存数据库
 */
@Database(
    entities = [NotificationEntity::class],
    version = 2,
    exportSchema = false,
)
abstract class NotificationDatabase : RoomDatabase() {
    abstract fun notificationDao(): NotificationDao
}
