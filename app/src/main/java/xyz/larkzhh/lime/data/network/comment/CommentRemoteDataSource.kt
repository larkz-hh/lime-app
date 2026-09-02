package xyz.larkzhh.lime.data.network.comment

import okhttp3.MultipartBody
import xyz.larkzhh.lime.data.network.ApiService
import xyz.larkzhh.lime.data.network.model.CommentData
import xyz.larkzhh.lime.data.network.model.CommentListResponse
import xyz.larkzhh.lime.data.network.model.PostCommentRequest
import xyz.larkzhh.lime.data.network.model.PostReplyRequest
import xyz.larkzhh.lime.data.network.model.ReplyData
import xyz.larkzhh.lime.data.network.model.ReplyListResponse
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 评论模块远端数据源
 */
@Singleton
class CommentRemoteDataSource @Inject constructor(
    private val apiService: ApiService,
) {

    /// 获取评论
    suspend fun getComments(
        noteId: Long,
        sort: String,
        cursor: String?,
        size: Int,
    ): Result<CommentListResponse> = runCatching {
        val response = apiService.getComments(noteId = noteId, sort = sort, cursor = cursor, size = size)
        check(response.code == 200 && response.data != null) { response.message }
        response.data
    }

    /// 发布评论
    suspend fun postComment(
        noteId: Long,
        content: String?,
        images: List<String>?,
        voiceUrl: String?,
        voiceDuration: Int?,
    ): Result<CommentData> = runCatching {
        val response = apiService.postComment(noteId, PostCommentRequest(content, images, voiceUrl, voiceDuration))
        check(response.code == 200 && response.data != null) { response.message }
        response.data
    }

    /// 获取回复
    suspend fun getReplies(commentId: Long, cursor: Long?, size: Int): Result<ReplyListResponse> = runCatching {
        val response = apiService.getReplies(commentId = commentId, cursor = cursor, size = size)
        check(response.code == 200 && response.data != null) { response.message }
        response.data
    }

    /// 发布回复
    suspend fun postReply(
        noteId: Long,
        commentId: Long,
        content: String?,
        images: List<String>?,
        replyToUserId: Long?,
        voiceUrl: String?,
        voiceDuration: Int?,
    ): Result<ReplyData> = runCatching {
        val response = apiService.postReply(
            noteId, commentId,
            PostReplyRequest(content, replyToUserId, images, voiceUrl, voiceDuration),
        )
        check(response.code == 200 && response.data != null) { response.message }
        response.data
    }

    /// 点赞评论、回复
    suspend fun likeComment(commentId: Long): Result<Unit> = runCatching {
        val response = apiService.likeComment(commentId)
        check(response.code == 200) { response.message }
    }

    /// 取消点赞评论、回复
    suspend fun unlikeComment(commentId: Long): Result<Unit> = runCatching {
        val response = apiService.unlikeComment(commentId)
        check(response.code == 200) { response.message }
    }

    /// 删除评论、回复
    suspend fun deleteComment(commentId: Long): Result<Unit> = runCatching {
        val response = apiService.deleteComment(commentId)
        check(response.code == 200) { response.message }
    }

    /// 上传评论图片
    suspend fun uploadCommentImage(part: MultipartBody.Part): Result<String> = runCatching {
        val response = apiService.uploadCommentImage(part)
        check(response.code == 200 && response.data != null) { response.message }
        response.data.url
    }

    /// 上传评论语音
    suspend fun uploadCommentVoice(part: MultipartBody.Part): Result<String> = runCatching {
        val response = apiService.uploadCommentVoice(part)
        check(response.code == 200 && response.data != null) { response.message }
        response.data.url
    }
}
