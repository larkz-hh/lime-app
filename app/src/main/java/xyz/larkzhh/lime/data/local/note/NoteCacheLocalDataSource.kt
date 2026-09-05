package xyz.larkzhh.lime.data.local.note

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import xyz.larkzhh.lime.data.local.UserDatabases
import xyz.larkzhh.lime.data.network.model.CommentData
import xyz.larkzhh.lime.data.network.model.CommentListResponse
import xyz.larkzhh.lime.data.network.model.NoteDetailData
import xyz.larkzhh.lime.data.network.model.ReplyData
import xyz.larkzhh.lime.data.network.model.ReplyListResponse
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 笔记详情本地数据源
 */
@Singleton
class NoteCacheLocalDataSource @Inject constructor(
    private val userDatabases: UserDatabases,
) {

    private val gson = Gson()

    private fun dao(): NoteCacheDao = userDatabases.noteCacheDao()

    // 获取本地笔记详情
    suspend fun getNoteDetail(noteId: Long): NoteDetailData? =
        dao().getNoteDetail(noteId)?.let { entity ->
            runCatching { gson.fromJson(entity.json, NoteDetailData::class.java) }.getOrNull()
        }

    /// 保存笔记详情
    suspend fun saveNoteDetail(noteId: Long, detail: NoteDetailData) {
        runCatching {
            dao().upsertNoteDetail(
                NoteDetailEntity(noteId, gson.toJson(detail), System.currentTimeMillis())
            )
        }
    }

    /// 删除笔记详情、评论及回复缓存
    suspend fun deleteNoteCache(noteId: Long) {
        runCatching {
            val commentIds = getComments(noteId)?.items?.map { it.id }.orEmpty()
            dao().deleteNoteCache(noteId, commentIds)
        }
    }

    // 获取本地评论
    suspend fun getComments(noteId: Long): CommentListResponse? =
        dao().getComments(noteId)?.let { entity ->
            runCatching {
                CommentListResponse(
                    items = gson.fromJson(entity.json, object : TypeToken<List<CommentData>>() {}.type),
                    nextCursor = entity.nextCursor,
                    hasMore = entity.hasMore,
                )
            }.getOrNull()
        }

    /// 保存评论
    suspend fun saveComments(noteId: Long, response: CommentListResponse) {
        runCatching {
            dao().upsertComments(
                CommentCacheEntity(
                    noteId = noteId,
                    json = gson.toJson(response.items),
                    nextCursor = response.nextCursor,
                    hasMore = response.hasMore,
                    cachedAt = System.currentTimeMillis(),
                )
            )
        }
    }

    // 获取本地回复
    suspend fun getReplies(commentId: Long): ReplyListResponse? =
        dao().getReplies(commentId)?.let { entity ->
            runCatching {
                ReplyListResponse(
                    items = gson.fromJson(entity.json, object : TypeToken<List<ReplyData>>() {}.type),
                    nextCursor = entity.nextCursor,
                    hasMore = entity.hasMore,
                )
            }.getOrNull()
        }

    /// 保存回复
    suspend fun saveReplies(commentId: Long, response: ReplyListResponse) {
        runCatching {
            dao().upsertReplies(
                ReplyCacheEntity(
                    commentId = commentId,
                    json = gson.toJson(response.items),
                    nextCursor = response.nextCursor,
                    hasMore = response.hasMore,
                    cachedAt = System.currentTimeMillis(),
                )
            )
        }
    }
}
