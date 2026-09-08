package xyz.larkzhh.lime.data.network.comment

import okhttp3.MultipartBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query
import xyz.larkzhh.lime.data.network.model.ApiResponse
import xyz.larkzhh.lime.data.network.model.CommentData
import xyz.larkzhh.lime.data.network.model.CommentListResponse
import xyz.larkzhh.lime.data.network.model.PostCommentRequest
import xyz.larkzhh.lime.data.network.model.PostReplyRequest
import xyz.larkzhh.lime.data.network.model.ReplyData
import xyz.larkzhh.lime.data.network.model.ReplyListResponse
import xyz.larkzhh.lime.data.network.model.UploadNoteImageResponse

interface CommentApi {

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

    /// 删除评论/回复
    @DELETE("api/comments/{commentId}")
    suspend fun deleteComment(@Path("commentId") commentId: Long): ApiResponse<Unit>

    /// 上传评论图片
    @Multipart
    @POST("api/comments/images")
    suspend fun uploadCommentImage(@Part file: MultipartBody.Part): ApiResponse<UploadNoteImageResponse>

    /// 上传评论语音
    @Multipart
    @POST("api/comments/voices")
    suspend fun uploadCommentVoice(@Part file: MultipartBody.Part): ApiResponse<UploadNoteImageResponse>
}
