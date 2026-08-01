package xyz.larkzhh.lime.data.network.model

data class HistoryFeedItem(
    val id: Long,
    val title: String?,
    val coverImage: String?,
    val likeCount: Int,
    val liked: Boolean,
    val author: FeedAuthor,
    val viewTime: String,
)

data class HistoryResponse(
    val items: List<HistoryFeedItem>,
    val nextCursor: Long?,
    val hasMore: Boolean,
)

data class DeleteHistoryRequest(
    val noteIds: List<Long>,
)

fun HistoryFeedItem.toFeedItem() = FeedItem(
    id = id,
    title = title,
    coverImage = coverImage,
    likeCount = likeCount,
    liked = liked,
    author = author,
)
