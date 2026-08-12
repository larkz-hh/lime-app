package xyz.larkzhh.lime.data.repository

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import xyz.larkzhh.lime.data.network.ApiService
import xyz.larkzhh.lime.data.network.model.CommentData
import xyz.larkzhh.lime.data.network.model.CommentListResponse
import xyz.larkzhh.lime.data.network.model.PostCommentRequest
import xyz.larkzhh.lime.data.network.model.PostReplyRequest
import xyz.larkzhh.lime.data.network.model.ReplyData
import xyz.larkzhh.lime.data.network.model.ReplyListResponse
import xyz.larkzhh.lime.domain.repository.CommentRepository
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CommentRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
    @param:ApplicationContext private val context: Context,
) : CommentRepository {

    /// 获取评论
    override suspend fun getComments(noteId: Long, sort: String, cursor: String?, size: Int): Result<CommentListResponse> = runCatching {
        val response = apiService.getComments(noteId = noteId, sort = sort, cursor = cursor, size = size)
        check(response.code == 200 && response.data != null) { response.message }
        response.data
    }

    /// 发送评论
    override suspend fun sentComment(noteId: Long, content: String?, images: List<String>?, voiceUrl: String?, voiceDuration: Int?): Result<CommentData> = runCatching {
        val response = apiService.postComment(noteId, PostCommentRequest(content, images, voiceUrl, voiceDuration))
        check(response.code == 200 && response.data != null) { response.message }
        response.data
    }

    /// 获取回复
    override suspend fun getReplies(commentId: Long, cursor: Long?, size: Int): Result<ReplyListResponse> = runCatching {
        val response = apiService.getReplies(commentId = commentId, cursor = cursor, size = size)
        check(response.code == 200 && response.data != null) { response.message }
        response.data
    }

    /// 发送回复
    override suspend fun sentReply(noteId: Long, commentId: Long, content: String?, images: List<String>?, replyToUserId: Long?, voiceUrl: String?, voiceDuration: Int?): Result<ReplyData> = runCatching {
        val response = apiService.postReply(noteId, commentId, PostReplyRequest(content, replyToUserId, images, voiceUrl, voiceDuration))
        check(response.code == 200 && response.data != null) { response.message }
        response.data
    }

    /// 点赞评论
    override suspend fun likeComment(commentId: Long): Result<Unit> = runCatching {
        val response = apiService.likeComment(commentId)
        check(response.code == 200) { response.message }
    }

    /// 取消点赞
    override suspend fun unlikeComment(commentId: Long): Result<Unit> = runCatching {
        val response = apiService.unlikeComment(commentId)
        check(response.code == 200) { response.message }
    }

    /// 删除评论/回复
    override suspend fun deleteComment(commentId: Long): Result<Unit> = runCatching {
        val response = apiService.deleteComment(commentId)
        check(response.code == 200) { response.message }
    }

    /// 上传评论图片
    override suspend fun uploadCommentImage(uri: Uri): Result<String> = runCatching {
        val bytes = context.contentResolver.openInputStream(uri)?.readBytes()
            ?: error("无法读取图片文件")
        val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
        val ext = when (mimeType) {
            "image/png" -> "png"
            "image/webp" -> "webp"
            "image/gif" -> "gif"
            else -> "jpg"
        }
        val requestBody = bytes.toRequestBody(mimeType.toMediaTypeOrNull())
        val part = MultipartBody.Part.createFormData("file", "upload.$ext", requestBody)
        val response = apiService.uploadCommentImage(part)
        check(response.code == 200 && response.data != null) { response.message }
        response.data.url
    }

    /// 上传评论语音
    override suspend fun uploadCommentVoice(file: File): Result<String> = runCatching {
        val bytes = file.readBytes()
        val requestBody = bytes.toRequestBody("audio/mp4".toMediaTypeOrNull())
        val part = MultipartBody.Part.createFormData("file", file.name, requestBody)
        val response = apiService.uploadCommentVoice(part)
        check(response.code == 200 && response.data != null) { response.message }
        response.data.url
    }
}
