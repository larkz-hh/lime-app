package xyz.larkzhh.lime.data.local.note

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 笔记详情缓存
 */
@Entity(tableName = "note_details")
data class NoteDetailEntity(
    @PrimaryKey val noteId: Long,
    val json: String,
    val cachedAt: Long,
)

/**
 * 评论区缓存实体
 */
@Entity(tableName = "comment_caches")
data class CommentCacheEntity(
    @PrimaryKey val noteId: Long,
    val json: String,
    val nextCursor: String?,
    val hasMore: Boolean,
    val cachedAt: Long,
)

/**
 * 回复缓存
 */
@Entity(tableName = "reply_caches")
data class ReplyCacheEntity(
    @PrimaryKey val commentId: Long,
    val json: String,
    val nextCursor: Long?,
    val hasMore: Boolean,
    val cachedAt: Long,
)
