package xyz.larkzhh.lime.data.network.model

/// 笔记搜索响应
data class NoteSearchResponse(
    val items: List<FeedItem>,
    val nextCursor: String?,
    val hasMore: Boolean,
)

/// 热搜词条
data class HotSearchItem(
    val keyword: String,
    val count: Int,
)

/// 上报搜索请求体
data class SearchReportRequest(
    val keyword: String,
)
