package xyz.larkzhh.lime.data.network

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
import xyz.larkzhh.lime.data.network.model.CommentData
import xyz.larkzhh.lime.data.network.model.CommentListResponse
import xyz.larkzhh.lime.data.network.model.DeleteHistoryRequest
import xyz.larkzhh.lime.data.network.model.FeedResponse
import xyz.larkzhh.lime.data.network.model.HistoryResponse
import xyz.larkzhh.lime.data.network.model.LoginRequest
import xyz.larkzhh.lime.data.network.model.NoteData
import xyz.larkzhh.lime.data.network.model.NoteDetailData
import xyz.larkzhh.lime.data.network.model.PostCommentRequest
import xyz.larkzhh.lime.data.network.model.PostReplyRequest
import xyz.larkzhh.lime.data.network.model.PublishNoteRequest
import xyz.larkzhh.lime.data.network.model.RefreshTokenRequest
import xyz.larkzhh.lime.data.network.model.RegisterRequest
import xyz.larkzhh.lime.data.network.model.ReplyData
import xyz.larkzhh.lime.data.network.model.ReplyListResponse
import xyz.larkzhh.lime.data.network.model.SendCodeRequest
import xyz.larkzhh.lime.data.network.model.TokenData
import xyz.larkzhh.lime.data.network.model.UpdateProfileRequest
import xyz.larkzhh.lime.data.network.model.UploadNoteImageResponse
import xyz.larkzhh.lime.data.network.model.UserData

interface ApiService {

    /// 发送验证码
    @POST("api/auth/send-code")
    suspend fun sendCode(@Body request: SendCodeRequest): ApiResponse<Unit>

    /// 登录
    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): ApiResponse<TokenData>

    /// 注册
    @POST("api/auth/register")
    suspend fun register(@Body request: RegisterRequest): ApiResponse<Unit>

    /// 刷新令牌
    @POST("api/auth/refresh")
    suspend fun refreshToken(@Body request: RefreshTokenRequest): ApiResponse<TokenData>

    /// 登出
    @POST("api/auth/logout")
    suspend fun logout(): ApiResponse<Unit>

    /// 获取当前用户信息
    @GET("api/user/me")
    suspend fun getMe(): ApiResponse<UserData>

    /// 修改个人资料
    @PUT("api/user/me")
    suspend fun updateMe(@Body request: UpdateProfileRequest): ApiResponse<UserData>

    /// 上传、更换头像
    @Multipart
    @POST("api/user/me/avatar")
    suspend fun uploadAvatar(@Part file: MultipartBody.Part): ApiResponse<UserData>

    /// 上传、更换背景图
    @Multipart
    @POST("api/user/me/background")
    suspend fun uploadBackground(@Part file: MultipartBody.Part): ApiResponse<UserData>

    /// 上传笔记图片
    @Multipart
    @POST("api/notes/images")
    suspend fun uploadNoteImage(@Part file: MultipartBody.Part): ApiResponse<UploadNoteImageResponse>

    /// 发布图文笔记
    @POST("api/notes")
    suspend fun publishNote(@Body request: PublishNoteRequest): ApiResponse<NoteData>

    /// 点赞笔记
    @POST("api/notes/{id}/like")
    suspend fun likeNote(@Path("id") id: Long): ApiResponse<Unit>

    /// 取消点赞笔记
    @DELETE("api/notes/{id}/like")
    suspend fun unlikeNote(@Path("id") id: Long): ApiResponse<Unit>

    /// 获取笔记详情
    @GET("api/notes/{id}")
    suspend fun getNoteDetail(@Path("id") id: Long): ApiResponse<NoteDetailData>

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

    /// 获取笔记评论列表
    @GET("api/notes/{noteId}/comments")
    suspend fun getComments(
        @Path("noteId") noteId: Long,
        @Query("sort") sort: String,
        @Query("cursor") cursor: String?,
        @Query("size") size: Int,
    ): ApiResponse<CommentListResponse>

    /// 发布评论
    @POST("api/notes/{noteId}/comments")
    suspend fun postComment(
        @Path("noteId") noteId: Long,
        @Body request: PostCommentRequest,
    ): ApiResponse<CommentData>

    /// 获取回复列表
    @GET("api/comments/{commentId}/replies")
    suspend fun getReplies(
        @Path("commentId") commentId: Long,
        @Query("cursor") cursor: Long?,
        @Query("size") size: Int,
    ): ApiResponse<ReplyListResponse>

    /// 发布回复
    @POST("api/notes/{noteId}/comments/{commentId}/replies")
    suspend fun postReply(
        @Path("noteId") noteId: Long,
        @Path("commentId") commentId: Long,
        @Body request: PostReplyRequest,
    ): ApiResponse<ReplyData>

    /// 点赞评论/回复
    @POST("api/comments/{commentId}/like")
    suspend fun likeComment(@Path("commentId") commentId: Long): ApiResponse<Unit>

    /// 取消点赞评论/回复
    @DELETE("api/comments/{commentId}/like")
    suspend fun unlikeComment(@Path("commentId") commentId: Long): ApiResponse<Unit>
}
