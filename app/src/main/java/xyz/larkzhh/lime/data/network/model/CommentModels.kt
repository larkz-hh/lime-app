package xyz.larkzhh.lime.data.network.model

data class ReplyData(
    val id: Long,
    val author: FeedAuthor,
    val replyToUserId: Long?,
    val replyToNickname: String?,
    val content: String? = null,
    val images: List<String>? = null,
    val likeCount: Int,
    val liked: Boolean,
    val isNoteAuthor: Boolean,
    val createTime: String,
    val ipLocation: String? = null,
)

data class CommentData(
    val id: Long,
    val author: FeedAuthor,
    val content: String? = null,
    val images: List<String>? = null,
    val likeCount: Int,
    val replyCount: Int,
    val liked: Boolean,
    val isNoteAuthor: Boolean,
    val createTime: String,
    val ipLocation: String? = null,
    val topReplies: List<ReplyData>?,
)

data class CommentListResponse(
    val items: List<CommentData>,
    val nextCursor: String?,
    val hasMore: Boolean,
)

data class ReplyListResponse(
    val items: List<ReplyData>,
    val nextCursor: Long?,
    val hasMore: Boolean,
)

data class PostCommentRequest(
    val content: String? = null,
    val images: List<String>? = null,
)

data class PostReplyRequest(
    val content: String? = null,
    val replyToUserId: Long? = null,
    val images: List<String>? = null,
)
