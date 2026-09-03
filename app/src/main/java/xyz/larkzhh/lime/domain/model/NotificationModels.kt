package xyz.larkzhh.lime.domain.model

/**
 * 站内通知类型
 */
enum class NotificationType(val code: Int) {
    LikeNote(1),// 点赞笔记
    Favorite(2),// 收藏
    Comment(3),// 评论
    Reply(4),// 回复
    Follow(5),// 关注
    LikeComment(6);// 点赞评论、回复

    companion object {
        fun fromCode(code: Int): NotificationType =
            entries.firstOrNull { it.code == code } ?: LikeNote
    }
}

/**
 * 消息页三个入口分类
 */
enum class NotificationCategory(
    val routeKey: String,
    val title: String,
    val types: Set<NotificationType>,
) {
    LikesFavorites(
        routeKey = "likes",
        title = "收到的赞和收藏",
        types = setOf(NotificationType.LikeNote, NotificationType.Favorite, NotificationType.LikeComment),
    ),
    Follows(
        routeKey = "follows",
        title = "新增关注",
        types = setOf(NotificationType.Follow),
    ),
    Comments(
        routeKey = "comments",
        title = "收到的评论与回复",
        types = setOf(NotificationType.Comment, NotificationType.Reply),
    );

    companion object {
        fun fromRouteKey(key: String?): NotificationCategory =
            entries.firstOrNull { it.routeKey == key } ?: LikesFavorites
    }
}

/// 信箱对应的后端 type
val NotificationCategory.typeParam: String
    get() = types.joinToString(",") { it.code.toString() }
