package xyz.larkzhh.lime.data.network.notification

import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import okhttp3.OkHttpClient
import xyz.larkzhh.lime.data.network.model.NotificationListResponse
import xyz.larkzhh.lime.data.network.model.UnreadCountData
import xyz.larkzhh.lime.data.network.notificationUnreadFlow
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * 站内通知远端数据源
 */
@Singleton
class NotificationRemoteDataSource @Inject constructor(
    private val apiService: NotificationApi,
    @Named("base_url") private val baseUrl: String,
    @Named("sse") private val sseClient: OkHttpClient,
    private val gson: Gson,
) {

    /// 通知列表
    suspend fun getNotifications(
        cursor: Long?,
        size: Int,
        type: String?
    ): Result<NotificationListResponse> = runCatching {
        val response = apiService.getNotifications(cursor = cursor, size = size, type = type)
        check(response.code == 200 && response.data != null) { response.message }
        response.data
    }

    /// 分组未读通知数
    suspend fun getUnreadCount(): Result<UnreadCountData> = runCatching {
        val response = apiService.getUnreadCount()
        check(response.code == 200 && response.data != null) { response.message }
        response.data
    }

    /// 标记单条已读
    suspend fun markRead(id: Long): Result<Unit> = runCatching {
        val response = apiService.markNotificationRead(id)
        check(response.code == 200) { response.message }
    }

    /// 批量标记已读
    suspend fun markAllRead(type: String?): Result<Unit> = runCatching {
        val response = apiService.markAllNotificationsRead(type = type)
        check(response.code == 200) { response.message }
    }

    /// 删除单条
    suspend fun delete(id: Long): Result<Unit> = runCatching {
        val response = apiService.deleteNotification(id)
        check(response.code == 200) { response.message }
    }

    /// 清空全部
    suspend fun clearAll(): Result<Unit> = runCatching {
        val response = apiService.clearAllNotifications()
        check(response.code == 200) { response.message }
    }

    /// SSE 分组未读推送流
    fun unreadStream(): Flow<UnreadCountData> =
        notificationUnreadFlow(
            client = sseClient,
            url = "${baseUrl}api/notifications/subscribe",
            gson = gson,
        )
}
