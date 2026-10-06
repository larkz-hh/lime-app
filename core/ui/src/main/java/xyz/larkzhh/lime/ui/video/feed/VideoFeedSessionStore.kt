package xyz.larkzhh.lime.ui.video.feed

import xyz.larkzhh.lime.data.network.model.FeedItem

/// 个人列表进入视频页时的预取负载
data class PersonalVideoPayload(
    val items: List<FeedItem>,// 原始列表
    val startIndex: Int,// 点击项列表下标
)

/// 视频页个人列表会话的进程内存储
object VideoFeedSessionStore {
    private val payloads = mutableMapOf<Long, PersonalVideoPayload>()

    fun put(key: Long, payload: PersonalVideoPayload) {
        payloads[key] = payload
    }

    fun take(key: Long): PersonalVideoPayload? = payloads.remove(key)
}
