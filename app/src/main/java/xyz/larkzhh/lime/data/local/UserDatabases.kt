package xyz.larkzhh.lime.data.local

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import dagger.hilt.android.qualifiers.ApplicationContext
import xyz.larkzhh.lime.data.local.chat.ChatDao
import xyz.larkzhh.lime.data.local.chat.ChatDatabase
import xyz.larkzhh.lime.data.local.feed.FeedDao
import xyz.larkzhh.lime.data.local.feed.FeedDatabase
import xyz.larkzhh.lime.data.local.note.NoteCacheDao
import xyz.larkzhh.lime.data.local.note.NoteCacheDatabase
import xyz.larkzhh.lime.data.local.notification.NotificationDao
import xyz.larkzhh.lime.data.local.notification.NotificationDatabase
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 按账号分片的 Room 数据库提供者
 * 多账号数据隔离
 * 退出登录保留各账号的库文件
 * 未登录时使用不带账号后缀的默认库
 */
@Singleton
class UserDatabases @Inject constructor(
    @ApplicationContext private val context: Context,
    private val tokenStorage: TokenStorage,
) {

    private val lock = Any()
    private val instances = mutableMapOf<String, RoomDatabase>()

    @Suppress("UNCHECKED_CAST")
    private fun <T : RoomDatabase> open(
        prefix: String,
        uid: Long? = tokenStorage.currentUserId,
        builder: (fileName: String) -> T,
    ): T = synchronized(lock) {
        val name = if (uid != null) "${prefix}_$uid" else prefix
        instances[name]?.let { return@synchronized it as T }
        val db = builder("$name.db")
        instances[name] = db
        db
    }

    /// 当前账号 AI 聊天库
    fun chatDao(): ChatDao =
        open("lime_chat") { name -> buildDb(ChatDatabase::class.java, name) }.chatDao()

    /// 当前账号信息流缓存库
    fun feedDao(): FeedDao =
        open("lime_feed") { name -> buildDb(FeedDatabase::class.java, name) }.feedDao()

    /// 当前账号笔记详情、评论缓存库
    fun noteCacheDao(): NoteCacheDao =
        open("lime_note_cache") { name -> buildDb(NoteCacheDatabase::class.java, name) }.noteCacheDao()

    /// 当前账号通知缓存库
    fun notificationDao(): NotificationDao =
        open("lime_notification") { name -> buildDb(NotificationDatabase::class.java, name) }.notificationDao()

    /// 指定账号通知缓存库
    fun notificationDaoFor(uid: Long?): NotificationDao =
        open("lime_notification", uid) { name -> buildDb(NotificationDatabase::class.java, name) }
            .notificationDao()

    /// 关闭并释放所有已打开的数据库实例
    fun closeAll() {
        synchronized(lock) {
            instances.values.forEach { it.close() }
            instances.clear()
        }
    }

    private fun <T : RoomDatabase> buildDb(clazz: Class<T>, fileName: String): T =
        Room.databaseBuilder(context, clazz, fileName)
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()
}
