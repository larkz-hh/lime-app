package xyz.larkzhh.lime.domain.repository

import kotlinx.coroutines.flow.StateFlow
import xyz.larkzhh.lime.data.network.model.NotificationData
import xyz.larkzhh.lime.data.network.model.NotificationListResponse
import xyz.larkzhh.lime.domain.model.NotificationCategory

/**
 * 站内通知数据仓库
 */
interface NotificationRepository {

    /// 本地缓存通知
    val items: StateFlow<List<NotificationData>>

    /// 各入口未读数
    val unreadByCategory: StateFlow<Map<NotificationCategory, Int>>

    /// 服务端总未读数
    val totalUnread: StateFlow<Int>

    /// 刷新拉取未读数、首页通知写入缓存
    suspend fun refresh(): Result<Unit>

    /// 分页拉取通知列表
    suspend fun loadPage(cursor: Long?, size: Int = 20, type: String? = null): Result<NotificationListResponse>

    /// 标记单条已读
    suspend fun markRead(id: Long): Result<Unit>

    /// 标记已读
    suspend fun markAllRead(category: NotificationCategory? = null): Result<Unit>

    /// 删除单条
    suspend fun delete(id: Long): Result<Unit>

    /// 清空全部
    suspend fun clearAll(): Result<Unit>

    /// 订阅 SSE 实时未读数推送
    fun startSse()

    /// 停止 SSE
    fun stopSse()
}
