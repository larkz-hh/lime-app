package xyz.larkzhh.lime.domain.repository

import android.net.Uri
import xyz.larkzhh.lime.data.network.model.CommentData
import xyz.larkzhh.lime.data.network.model.CommentListResponse
import xyz.larkzhh.lime.data.network.model.ReplyData
import xyz.larkzhh.lime.data.network.model.ReplyListResponse
import java.io.File

interface CommentRepository {
    suspend fun getComments(noteId: Long, sort: String, cursor: String?, size: Int): Result<CommentListResponse>
    suspend fun sentComment(noteId: Long, content: String?, images: List<String>?, voiceUrl: String?, voiceDuration: Int?): Result<CommentData>
    suspend fun getReplies(commentId: Long, cursor: Long?, size: Int): Result<ReplyListResponse>
    suspend fun sentReply(noteId: Long, commentId: Long, content: String?, images: List<String>?, replyToUserId: Long?, voiceUrl: String?, voiceDuration: Int?): Result<ReplyData>
    suspend fun likeComment(commentId: Long): Result<Unit>
    suspend fun unlikeComment(commentId: Long): Result<Unit>
    suspend fun deleteComment(commentId: Long): Result<Unit>
    suspend fun uploadCommentImage(uri: Uri): Result<String>
    suspend fun uploadCommentVoice(file: File): Result<String>
}
