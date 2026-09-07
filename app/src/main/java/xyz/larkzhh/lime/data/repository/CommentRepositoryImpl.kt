package xyz.larkzhh.lime.data.repository

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import xyz.larkzhh.lime.data.local.note.NoteCacheLocalDataSource
import xyz.larkzhh.lime.data.network.comment.CommentRemoteDataSource
import xyz.larkzhh.lime.data.network.model.CommentData
import xyz.larkzhh.lime.data.network.model.CommentListResponse
import xyz.larkzhh.lime.data.network.model.ReplyData
import xyz.larkzhh.lime.data.network.model.ReplyListResponse
import xyz.larkzhh.lime.domain.repository.CommentRepository
import xyz.larkzhh.lime.util.media.ImageCompressor
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 评论仓库实现
 */
@Singleton
class CommentRepositoryImpl @Inject constructor(
    private val commentRemoteDataSource: CommentRemoteDataSource,
    private val noteCacheLocalDataSource: NoteCacheLocalDataSource,
    @param:ApplicationContext private val context: Context,
) : CommentRepository {

    /// 获取评论
    override suspend fun getComments(noteId: Long, sort: String, cursor: String?, size: Int): Result<CommentListResponse> =
        commentRemoteDataSource.getComments(noteId, sort, cursor, size)

    /// 发送评论
    override suspend fun sentComment(noteId: Long, content: String?, images: List<String>?, voiceUrl: String?, voiceDuration: Int?): Result<CommentData> =
        commentRemoteDataSource.postComment(noteId, content, images, voiceUrl, voiceDuration)

    /// 获取回复
    override suspend fun getReplies(commentId: Long, cursor: Long?, size: Int): Result<ReplyListResponse> =
        commentRemoteDataSource.getReplies(commentId, cursor, size)

    /// 发送回复
    override suspend fun sentReply(noteId: Long, commentId: Long, content: String?, images: List<String>?, replyToUserId: Long?, voiceUrl: String?, voiceDuration: Int?): Result<ReplyData> =
        commentRemoteDataSource.postReply(noteId, commentId, content, images, replyToUserId, voiceUrl, voiceDuration)

    /// 点赞评论
    override suspend fun likeComment(commentId: Long): Result<Unit> =
        commentRemoteDataSource.likeComment(commentId)

    /// 取消点赞
    override suspend fun unlikeComment(commentId: Long): Result<Unit> =
        commentRemoteDataSource.unlikeComment(commentId)

    /// 删除评论/回复
    override suspend fun deleteComment(commentId: Long): Result<Unit> =
        commentRemoteDataSource.deleteComment(commentId)

    /// 上传评论图片
    override suspend fun uploadCommentImage(uri: Uri): Result<String> {
        val image = runCatching { ImageCompressor.compress(context, uri) }
            .getOrElse { return Result.failure(it) }
        val requestBody = image.bytes.toRequestBody(image.mimeType.toMediaTypeOrNull())
        val part = MultipartBody.Part.createFormData("file", "upload.${image.ext}", requestBody)
        return commentRemoteDataSource.uploadCommentImage(part)
    }

    /// 上传评论语音
    override suspend fun uploadCommentVoice(file: File): Result<String> {
        val bytes = runCatching { file.readBytes() }
            .getOrElse { return Result.failure(it) }
        val requestBody = bytes.toRequestBody("audio/mp4".toMediaTypeOrNull())
        val part = MultipartBody.Part.createFormData("file", file.name, requestBody)
        return commentRemoteDataSource.uploadCommentVoice(part)
    }

    /// 读取本地缓存评论
    override suspend fun getCachedComments(noteId: Long): CommentListResponse? =
        noteCacheLocalDataSource.getComments(noteId)

    /// 保存评论缓存
    override suspend fun saveCommentsCache(noteId: Long, response: CommentListResponse) {
        noteCacheLocalDataSource.saveComments(noteId, response)
    }

    /// 读取本地缓存回复
    override suspend fun getCachedReplies(commentId: Long): ReplyListResponse? =
        noteCacheLocalDataSource.getReplies(commentId)

    /// 保存回复缓存
    override suspend fun saveRepliesCache(commentId: Long, response: ReplyListResponse) {
        noteCacheLocalDataSource.saveReplies(commentId, response)
    }
}
