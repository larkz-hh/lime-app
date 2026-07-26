package xyz.larkzhh.lime.data.network.model

data class UploadNoteImageResponse(
    val url: String,
)

data class NoteImageRequest(
    val url: String,
    val sortOrder: Int,
)

data class PublishNoteRequest(
    val title: String?,
    val content: String?,
    val images: List<NoteImageRequest>,
    val status: Int = 1,  // 0=草稿，1=已发布
)

data class NoteImageData(
    val id: Long,
    val url: String,
    val sortOrder: Int,
)

data class NoteData(
    val id: Long,
    val userId: Long,
    val title: String?,
    val content: String?,
    val status: Int,
    val images: List<NoteImageData>,
    val createTime: String,
    val updateTime: String,
)

data class FeedAuthor(
    val id: Long,
    val nickname: String,
    val avatar: String?,
)

data class NoteDetailData(
    val id: Long,
    val title: String?,
    val content: String?,
    val status: Int,
    val images: List<NoteImageData>,
    val likeCount: Int,
    val favCount: Int,
    val viewCount: Int,
    val liked: Boolean,
    val favorited: Boolean,
    val author: FeedAuthor,
    val createTime: String,
    val updateTime: String,
)

data class FeedItem(
    val id: Long,
    val title: String?,
    val coverImage: String?,
    val likeCount: Int,
    val liked: Boolean,
    val author: FeedAuthor,// 作者详情
)

data class FeedResponse(
    val items: List<FeedItem>,
    val nextCursor: Long?,
    val hasMore: Boolean,
)
