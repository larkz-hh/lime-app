package xyz.larkzhh.lime.data.repository

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import xyz.larkzhh.lime.data.local.notification.NotificationLocalDataSource
import xyz.larkzhh.lime.data.network.model.NotificationData
import xyz.larkzhh.lime.data.network.model.NotificationListResponse
import xyz.larkzhh.lime.data.network.model.UnreadCountData
import xyz.larkzhh.lime.data.network.notification.NotificationRemoteDataSource
import xyz.larkzhh.lime.domain.model.NotificationCategory
import xyz.larkzhh.lime.domain.model.typeParam
import xyz.larkzhh.lime.domain.repository.NotificationRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 站内通知仓库实现
 */
@Singleton
class NotificationRepositoryImpl @Inject constructor(
    private val remote: NotificationRemoteDataSource,
    private val local: NotificationLocalDataSource,
) : NotificationRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var sseJob: Job? = null

    /// 服务端分组未读数
    private val _unread = MutableStateFlow(UnreadCountData())

    /// 服务端总未读数
    override val totalUnread: StateFlow<Int> = _unread
        .map { it.total }
        .stateIn(scope, SharingStarted.Eagerly, 0)

    /// 各入口未读数
    override val unreadByCategory: StateFlow<Map<NotificationCategory, Int>> = _unread
        .map { data ->
            mapOf(
                NotificationCategory.LikesFavorites to data.likeFav,
                NotificationCategory.Follows to data.follow,
                NotificationCategory.Comments to data.comment,
            )
        }
        .stateIn(scope, SharingStarted.Eagerly, emptyMap())

    /// 本地缓存通知
    override val items: StateFlow<List<NotificationData>> = local.observeAll()
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    /// 拉取分组未读数
    override suspend fun refresh(): Result<Unit> = runCatching {
        refreshUnread()
    }

    /// 分页拉取通知列表
    override suspend fun loadPage(cursor: Long?, size: Int, type: String?): Result<NotificationListResponse> = runCatching {
        val data = remote.getNotifications(cursor, size, type).getOrThrow()
        local.upsert(data.items)
        data
    }

    /// 标记单条已读
    override suspend fun markRead(id: Long): Result<Unit> {
        runCatching { local.markRead(id) }
        val result = remote.markRead(id)
        refreshUnread()
        return result
    }

    /// 标记已读
    override suspend fun markAllRead(category: NotificationCategory?): Result<Unit> {
        if (category == null) {
            // 全部信箱已读
            runCatching { local.markAllRead() }
            remote.markAllRead(type = null)
        } else {
            // 清空当前信箱
            runCatching { local.markAllReadByTypes(category.types.map { it.code }) }
            remote.markAllRead(type = category.typeParam)
        }
        refreshUnread()
        return Result.success(Unit)
    }

    /// 删除单条
    override suspend fun delete(id: Long): Result<Unit> {
        runCatching { local.delete(id) }
        val result = remote.delete(id)
        refreshUnread()
        return result
    }

    /// 清空全部
    override suspend fun clearAll(): Result<Unit> {
        runCatching { local.clearAll() }
        val result = remote.clearAll()
        refreshUnread()
        return result
    }

    /// 订阅 SSE 实时未读数推送
    override fun startSse() {
        if (sseJob?.isActive == true) return
        sseJob = scope.launch {
            remote.unreadStream().collect { unread ->
                _unread.value = unread
            }
        }
    }

    /// 停止 SSE
    override fun stopSse() {
        sseJob?.cancel()
        sseJob = null
    }

    /// 拉取分组未读数并更新内存
    private suspend fun refreshUnread() {
        remote.getUnreadCount().onSuccess { _unread.value = it }
    }
}
