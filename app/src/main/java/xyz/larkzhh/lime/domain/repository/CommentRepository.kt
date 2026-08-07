package xyz.larkzhh.lime.domain.repository

import xyz.larkzhh.lime.data.network.model.CommentData
import xyz.larkzhh.lime.data.network.model.CommentListResponse
import xyz.larkzhh.lime.data.network.model.ReplyData
import xyz.larkzhh.lime.data.network.model.ReplyListResponse

interface CommentRepository {
    suspend fun getComments(noteId: Long, sort: String, cursor: String?, size: Int): Result<CommentListResponse>
    suspend fun sentComment(noteId: Long, content: String): Result<CommentData>
    suspend fun getReplies(commentId: Long, cursor: Long?, size: Int): Result<ReplyListResponse>
    suspend fun sentReply(noteId: Long, commentId: Long, content: String, replyToUserId: Long?): Result<ReplyData>
    suspend fun likeComment(commentId: Long): Result<Unit>
    suspend fun unlikeComment(commentId: Long): Result<Unit>
}
