package xyz.larkzhh.lime.data.network.model

/// 单条弹幕
data class DanmakuData(
    val id: Long,
    val content: String,
    val videoTimeMs: Long,// 弹幕出现时间点
    val color: String? = null,
    val author: FeedAuthor,
    val createTime: String,
)

/// 弹幕列表响应
data class DanmakuListResponse(
    val items: List<DanmakuData>,
    val count: Int,
)

/// 发弹幕请求
data class PostDanmakuRequest(
    val content: String,
    val videoTimeMs: Long,
    val color: String? = null,
)
