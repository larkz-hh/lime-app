package xyz.larkzhh.lime.data.local.feed

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

/**
 * 信息流缓存 DAO
 */
@Dao
interface FeedDao {

    /// 提供分页数据源
    @Query("SELECT * FROM feed_items WHERE feedKey = :feedKey ORDER BY sortIndex ASC")
    fun pagingSource(feedKey: String): PagingSource<Int, FeedItemEntity>

    /// 批量插入或替换数据
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<FeedItemEntity>)

    /// 清空指定信息流数据
    @Query("DELETE FROM feed_items WHERE feedKey = :feedKey")
    suspend fun clear(feedKey: String)

    /// 删除某条笔记
    @Query("DELETE FROM feed_items WHERE noteId = :noteId")
    suspend fun deleteByNoteId(noteId: Long)

    /// 更新笔记在信息流缓存中的快照
    @Query("UPDATE feed_items SET json = :json WHERE noteId = :noteId")
    suspend fun updateJsonByNoteId(noteId: Long, json: String)

    /// 取某条笔记在信息流缓存里的全部快照
    @Query("SELECT * FROM feed_items WHERE noteId = :noteId")
    suspend fun getByNoteId(noteId: Long): List<FeedItemEntity>

    /// 获取当前分页游标
    @Query("SELECT * FROM feed_cursors WHERE feedKey = :feedKey")
    suspend fun getCursor(feedKey: String): FeedCursorEntity?

    /// 插入或更新分页游标
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCursor(cursor: FeedCursorEntity)

    /// 替换整页数据与游标
    @Transaction
    suspend fun replacePage(feedKey: String, items: List<FeedItemEntity>, cursor: Long?) {
        clear(feedKey)
        insertAll(items)
        upsertCursor(FeedCursorEntity(feedKey, cursor))
    }

    /// 追加一页
    @Transaction
    suspend fun appendPage(feedKey: String, items: List<FeedItemEntity>, cursor: Long?) {
        insertAll(items)
        upsertCursor(FeedCursorEntity(feedKey, cursor))
    }
}
