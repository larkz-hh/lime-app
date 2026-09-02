package xyz.larkzhh.lime.data.local.note

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

/**
 * 笔记详情本地缓存 DAO
 */
@Dao
interface NoteCacheDao {

    /// 获取笔记详情
    @Query("SELECT * FROM note_details WHERE noteId = :noteId")
    suspend fun getNoteDetail(noteId: Long): NoteDetailEntity?

    /// 插入笔记详情
    @Upsert
    suspend fun upsertNoteDetail(entity: NoteDetailEntity)

    // 获取评论
    @Query("SELECT * FROM comment_caches WHERE noteId = :noteId")
    suspend fun getComments(noteId: Long): CommentCacheEntity?

    /// 插入评论
    @Upsert
    suspend fun upsertComments(entity: CommentCacheEntity)

    /// 清空评论
    @Query("DELETE FROM comment_caches WHERE noteId = :noteId")
    suspend fun clearComments(noteId: Long)

    /// 获取回复
    @Query("SELECT * FROM reply_caches WHERE commentId = :commentId")
    suspend fun getReplies(commentId: Long): ReplyCacheEntity?

    /// 插入回复
    @Upsert
    suspend fun upsertReplies(entity: ReplyCacheEntity)

    /// 清空回复
    @Query("DELETE FROM reply_caches WHERE commentId = :commentId")
    suspend fun clearReplies(commentId: Long)
}
