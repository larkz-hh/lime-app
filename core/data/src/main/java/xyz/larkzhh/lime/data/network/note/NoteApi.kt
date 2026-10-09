package xyz.larkzhh.lime.data.network.note

import okhttp3.MultipartBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.HTTP
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query
import xyz.larkzhh.lime.data.network.model.ApiResponse
import xyz.larkzhh.lime.data.network.model.DeleteHistoryRequest
import xyz.larkzhh.lime.data.network.model.FeedResponse
import xyz.larkzhh.lime.data.network.model.HistoryResponse
import xyz.larkzhh.lime.data.network.model.NoteData
import xyz.larkzhh.lime.data.network.model.NoteDetailData
import xyz.larkzhh.lime.data.network.model.PublishNoteRequest
import xyz.larkzhh.lime.data.network.model.PublishVideoNoteRequest
import xyz.larkzhh.lime.data.network.model.UploadNoteImageResponse

interface NoteApi {

    /// 上传笔记图片
    @Multipart
    @POST("api/notes/images")
    suspend fun uploadNoteImage(@Part file: MultipartBody.Part): ApiResponse<UploadNoteImageResponse>

    /// 上传笔记视频
    @Multipart
    @POST("api/notes/videos")
    suspend fun uploadNoteVideo(@Part file: MultipartBody.Part): ApiResponse<UploadNoteImageResponse>

    /// 发布图文笔记
    @POST("api/notes")
    suspend fun publishNote(@Body request: PublishNoteRequest): ApiResponse<NoteData>

    /// 发布视频笔记
    @POST("api/notes")
    suspend fun publishVideoNote(@Body request: PublishVideoNoteRequest): ApiResponse<NoteData>

    /// 编辑图文笔记
    @PUT("api/notes/{id}")
    suspend fun updateNote(@Path("id") id: Long, @Body request: PublishNoteRequest): ApiResponse<NoteData>

    /// 编辑视频笔记
    @PUT("api/notes/{id}")
    suspend fun updateVideoNote(@Path("id") id: Long, @Body request: PublishVideoNoteRequest): ApiResponse<NoteData>

    /// 删除笔记
    @DELETE("api/notes/{id}")
    suspend fun deleteNote(@Path("id") id: Long): ApiResponse<Unit>

    /// 点赞笔记
    @POST("api/notes/{id}/like")
    suspend fun likeNote(@Path("id") id: Long): ApiResponse<Unit>

    /// 取消点赞笔记
    @DELETE("api/notes/{id}/like")
    suspend fun unlikeNote(@Path("id") id: Long): ApiResponse<Unit>

    /// 获取笔记详情
    @GET("api/notes/{id}")
    suspend fun getNoteDetail(
        @Path("id") id: Long,
        @Query("noView") noView: Boolean = false,
    ): ApiResponse<NoteDetailData>

    /// 收藏笔记
    @POST("api/notes/{id}/favorite")
    suspend fun favoriteNote(@Path("id") id: Long): ApiResponse<Unit>

    /// 取消收藏笔记
    @DELETE("api/notes/{id}/favorite")
    suspend fun unfavoriteNote(@Path("id") id: Long): ApiResponse<Unit>

    /// 获取信息流
    @GET("api/notes/feed")
    suspend fun getFeed(
        @Query("cursor") cursor: Long?,
        @Query("size") size: Int,
    ): ApiResponse<FeedResponse>

    /// 获取关注动态
    @GET("api/notes/following-feed")
    suspend fun getFollowingFeed(
        @Query("cursor") cursor: Long?,
        @Query("size") size: Int,
    ): ApiResponse<FeedResponse>

    /// 获取视频信息流
    @GET("api/notes/video-feed")
    suspend fun getVideoFeed(
        @Query("cursor") cursor: Long?,
        @Query("seedNoteId") seedNoteId: Long?,
        @Query("orientation") orientation: String?,
        @Query("size") size: Int,
    ): ApiResponse<FeedResponse>

    /// 获取指定用户的笔记列表
    @GET("api/notes/user/{userId}")
    suspend fun getUserNotes(
        @Path("userId") userId: Long,
        @Query("status") status: String = "published",
        @Query("cursor") cursor: Long?,
        @Query("size") size: Int,
    ): ApiResponse<FeedResponse>

    /// 获取指定用户的点赞列表
    @GET("api/notes/user/{userId}/likes")
    suspend fun getUserLikes(
        @Path("userId") userId: Long,
        @Query("cursor") cursor: Long?,
        @Query("size") size: Int,
    ): ApiResponse<FeedResponse>

    /// 获取指定用户的收藏列表
    @GET("api/notes/user/{userId}/favorites")
    suspend fun getUserFavorites(
        @Path("userId") userId: Long,
        @Query("cursor") cursor: Long?,
        @Query("size") size: Int,
    ): ApiResponse<FeedResponse>

    /// 获取浏览历史
    @GET("api/notes/history")
    suspend fun getHistory(
        @Query("cursor") cursor: Long?,
        @Query("size") size: Int,
    ): ApiResponse<HistoryResponse>

    /// 删除浏览历史条目
    @HTTP(method = "DELETE", path = "api/notes/history", hasBody = true)
    suspend fun deleteHistory(@Body request: DeleteHistoryRequest): ApiResponse<Unit>

    /// 清空全部浏览历史
    @DELETE("api/notes/history/all")
    suspend fun deleteHistoryAll(): ApiResponse<Unit>
}
