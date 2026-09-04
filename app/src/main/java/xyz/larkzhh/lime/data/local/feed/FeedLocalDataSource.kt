package xyz.larkzhh.lime.data.local.feed

import androidx.paging.PagingSource
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 信息流本地数据源
 */
@Singleton
class FeedLocalDataSource @Inject constructor(
    private val feedDao: FeedDao,
) {

    /// 提供分页数据源
    fun pagingSource(feedKey: String): PagingSource<Int, FeedItemEntity> =
        feedDao.pagingSource(feedKey)

    /// 批量插入或替换数据
    suspend fun getCursor(feedKey: String): FeedCursorEntity? = feedDao.getCursor(feedKey)

    /// 替换整页数据与游标
    suspend fun replacePage(feedKey: String, items: List<FeedItemEntity>, cursor: Long?) =
        feedDao.replacePage(feedKey, items, cursor)

    /// 追加一页
    suspend fun appendPage(feedKey: String, items: List<FeedItemEntity>, cursor: Long?) =
        feedDao.appendPage(feedKey, items, cursor)

    /// 从信息流缓存中删除某条笔记
    suspend fun deleteByNoteId(noteId: Long) = feedDao.deleteByNoteId(noteId)

    /// 更新笔记在信息流缓存中的快照
    suspend fun updateNoteItem(noteId: Long, json: String) =
        feedDao.updateJsonByNoteId(noteId, json)

    /// 取某条笔记在信息流缓存里的全部快照
    suspend fun getNoteItems(noteId: Long): List<FeedItemEntity> =
        feedDao.getByNoteId(noteId)
}
