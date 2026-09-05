package xyz.larkzhh.lime.data.local.feed

import androidx.paging.PagingSource
import xyz.larkzhh.lime.data.local.UserDatabases
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 信息流本地数据源
 */
@Singleton
class FeedLocalDataSource @Inject constructor(
    private val userDatabases: UserDatabases,
) {

    private fun dao(): FeedDao = userDatabases.feedDao()

    /// 提供分页数据源
    fun pagingSource(feedKey: String): PagingSource<Int, FeedItemEntity> =
        dao().pagingSource(feedKey)

    /// 批量插入或替换数据
    suspend fun getCursor(feedKey: String): FeedCursorEntity? = dao().getCursor(feedKey)

    /// 替换整页数据与游标
    suspend fun replacePage(feedKey: String, items: List<FeedItemEntity>, cursor: Long?) =
        dao().replacePage(feedKey, items, cursor)

    /// 追加一页
    suspend fun appendPage(feedKey: String, items: List<FeedItemEntity>, cursor: Long?) =
        dao().appendPage(feedKey, items, cursor)

    /// 从信息流缓存中删除某条笔记
    suspend fun deleteByNoteId(noteId: Long) = dao().deleteByNoteId(noteId)

    /// 更新笔记在信息流缓存中的快照
    suspend fun updateNoteItem(noteId: Long, json: String) =
        dao().updateJsonByNoteId(noteId, json)

    /// 取某条笔记在信息流缓存里的全部快照
    suspend fun getNoteItems(noteId: Long): List<FeedItemEntity> =
        dao().getByNoteId(noteId)
}
