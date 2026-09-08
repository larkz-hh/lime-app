package xyz.larkzhh.lime.data.local.notification

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/**
 * 站内通知缓存 DAO
 */
@Dao
interface NotificationDao {

    /// 观察全部通知，按时间倒序
    @Query("SELECT * FROM notifications ORDER BY createTime DESC, id DESC")
    fun observeAll(): Flow<List<NotificationEntity>>

    /// 同步读取最近若干条
    @Query("SELECT * FROM notifications ORDER BY createTime DESC, id DESC LIMIT :limit")
    suspend fun getRecent(limit: Int): List<NotificationEntity>

    /// 按类型列表批量标记已读
    @Query("UPDATE notifications SET isRead = 1 WHERE isRead = 0 AND type IN (:types)")
    suspend fun markAllReadByTypes(types: List<Int>)

    /// 批量写入或更新
    @Upsert
    suspend fun upsert(items: List<NotificationEntity>)

    /// 标记单条已读
    @Query("UPDATE notifications SET isRead = 1 WHERE id = :id")
    suspend fun markRead(id: Long)

    /// 全部标记已读
    @Query("UPDATE notifications SET isRead = 1 WHERE isRead = 0")
    suspend fun markAllRead()

    /// 删除单条
    @Query("DELETE FROM notifications WHERE id = :id")
    suspend fun delete(id: Long)

    /// 清空全部
    @Query("DELETE FROM notifications")
    suspend fun clearAll()
}
