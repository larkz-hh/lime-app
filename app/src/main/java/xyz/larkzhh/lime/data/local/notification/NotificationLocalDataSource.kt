package xyz.larkzhh.lime.data.local.notification

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import xyz.larkzhh.lime.data.mapper.toData
import xyz.larkzhh.lime.data.mapper.toEntity
import xyz.larkzhh.lime.data.network.model.NotificationData
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 站内通知本地数据源
 */
@Singleton
class NotificationLocalDataSource @Inject constructor(
    private val dao: NotificationDao,
) {

    /// 观察全部通知
    fun observeAll(): Flow<List<NotificationData>> =
        dao.observeAll().map { list -> list.map { it.toData() } }

    /// 按类型列表批量标记已读
    suspend fun markAllReadByTypes(types: List<Int>) = dao.markAllReadByTypes(types)

    /// 批量写入或更新
    suspend fun upsert(items: List<NotificationData>) =
        dao.upsert(items.map { it.toEntity() })

    /// 标记单条已读
    suspend fun markRead(id: Long) = dao.markRead(id)

    /// 全部标记已读
    suspend fun markAllRead() = dao.markAllRead()

    /// 删除单条
    suspend fun delete(id: Long) = dao.delete(id)

    /// 清空全部
    suspend fun clearAll() = dao.clearAll()
}
