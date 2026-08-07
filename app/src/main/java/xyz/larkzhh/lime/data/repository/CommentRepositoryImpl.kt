package xyz.larkzhh.lime.data.repository

import xyz.larkzhh.lime.data.network.ApiService
import xyz.larkzhh.lime.data.network.model.CommentData
import xyz.larkzhh.lime.data.network.model.CommentListResponse
import xyz.larkzhh.lime.data.network.model.PostCommentRequest
import xyz.larkzhh.lime.data.network.model.PostReplyRequest
import xyz.larkzhh.lime.data.network.model.ReplyData
import xyz.larkzhh.lime.data.network.model.ReplyListResponse
import xyz.larkzhh.lime.domain.repository.CommentRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CommentRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
) : CommentRepository {

    /// 获取评论
    override suspend fun getComments(noteId: Long, sort: String, cursor: String?, size: Int): Result<CommentListResponse> = runCatching {
        val response = apiService.getComments(noteId = noteId, sort = sort, cursor = cursor, size = size)
        check(response.code == 200 && response.data != null) { response.message }
        response.data
    }

    /// 发送评论
    override suspend fun sentComment(noteId: Long, content: String): Result<CommentData> = runCatching {
        val response = apiService.postComment(noteId, PostCommentRequest(content))
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
    override suspend fun sentReply(noteId: Long, commentId: Long, content: String, replyToUserId: Long?): Result<ReplyData> = runCatching {
        val response = apiService.postReply(noteId, commentId, PostReplyRequest(content, replyToUserId))
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
}
