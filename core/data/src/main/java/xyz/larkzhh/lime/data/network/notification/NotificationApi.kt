package xyz.larkzhh.lime.data.network.notification

import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query
import xyz.larkzhh.lime.data.network.model.ApiResponse
import xyz.larkzhh.lime.data.network.model.NotificationListResponse
import xyz.larkzhh.lime.data.network.model.UnreadCountData

interface NotificationApi {

    /// 站内通知列表
    @GET("api/notifications")
    suspend fun getNotifications(
        @Query("cursor") cursor: Long?,
        @Query("size") size: Int,
        @Query("type") type: String?,
    ): ApiResponse<NotificationListResponse>

    /// 未读通知数
    @GET("api/notifications/unread-count")
    suspend fun getUnreadCount(): ApiResponse<UnreadCountData>

    /// 标记单条通知已读
    @PUT("api/notifications/{id}/read")
    suspend fun markNotificationRead(@Path("id") id: Long): ApiResponse<Unit>

    /// 全部标记已读
    @PUT("api/notifications/read-all")
    suspend fun markAllNotificationsRead(@Query("type") type: String?): ApiResponse<Unit>

    /// 删除单条通知
    @DELETE("api/notifications/{id}")
    suspend fun deleteNotification(@Path("id") id: Long): ApiResponse<Unit>

    /// 清空全部通知
    @DELETE("api/notifications/all")
    suspend fun clearAllNotifications(): ApiResponse<Unit>
}
