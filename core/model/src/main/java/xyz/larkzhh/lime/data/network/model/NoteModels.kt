package xyz.larkzhh.lime.data.network.model

data class UploadNoteImageResponse(
    val url: String,
)

/// 图片尺寸
data class ImageSize(val width: Int, val height: Int)

data class NoteImageRequest(
    val url: String,
    val sortOrder: Int,
    val width: Int? = null,
    val height: Int? = null,
)

data class PublishNoteRequest(
    val title: String?,
    val content: String?,
    val images: List<NoteImageRequest>,
    val status: Int = 1,  // 0=草稿，1=已发布
)

/// 视频笔记的视频字段
data class VideoRequest(
    val url: String,
    val durationMs: Long,
    val width: Int,
    val height: Int,
    val coverUrl: String?,
    val coverWidth: Int? = null,
    val coverHeight: Int? = null,
)

/// 发布视频笔记请求
data class PublishVideoNoteRequest(
    val title: String?,
    val content: String?,
    val noteType: Int = 2,// 视频类型
    val video: VideoRequest,
    val status: Int = 1,  // 0=草稿，1=已发布
)

data class NoteImageData(
    val id: Long,
    val url: String,
    val sortOrder: Int,
    val width: Int? = null,
    val height: Int? = null,
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
    val isFollowing: Boolean? = null,
    val isFollowedBack: Boolean? = null,
)

/// 视频笔记的视频信息响应
data class VideoInfo(
    val playUrl: String,
    val coverUrl: String?,
    val durationMs: Long,
    val width: Int,
    val height: Int,
    val orientation: String,
)

/// 视频朝向
enum class VideoOrientation { PORTRAIT, LANDSCAPE }

fun VideoInfo.orientationEnum() =
    if (orientation.equals("LANDSCAPE", true)) VideoOrientation.LANDSCAPE else VideoOrientation.PORTRAIT

data class NoteDetailData(
    val id: Long,
    val title: String?,
    val content: String?,
    val status: Int,
    val images: List<NoteImageData>,
    val likeCount: Int,
    val favCount: Int,
    val viewCount: Int,
    val commentCount: Int = 0,
    val liked: Boolean,
    val favorited: Boolean,
    val author: FeedAuthor,
    val createTime: String? = null,
    val updateTime: String? = null,
    val noteType: Int = 1,// 1=图文，2=视频
    val video: VideoInfo? = null,
)

data class FeedItem(
    val id: Long,
    val title: String?,
    val coverImage: String?,
    val coverWidth: Int? = null,// 封面宽
    val coverHeight: Int? = null,// 封面高
    val likeCount: Int,
    val liked: Boolean,
    val author: FeedAuthor,
    val viewCount: Int? = null,// 本人才有值
    val noteType: Int = 1,// 1=图文，2=视频
    val video: VideoInfo? = null,
)

data class FeedResponse(
    val items: List<FeedItem>,
    val nextCursor: Long?,
    val hasMore: Boolean,
)
