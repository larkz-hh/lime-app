package xyz.larkzhh.lime.domain.repository

import android.net.Uri
import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import xyz.larkzhh.lime.data.network.model.FeedItem
import xyz.larkzhh.lime.data.network.model.FeedResponse
import xyz.larkzhh.lime.data.network.model.HistoryResponse
import xyz.larkzhh.lime.data.network.model.ImageSize
import xyz.larkzhh.lime.data.network.model.NoteDetailData

/**
 *  笔记数据仓库接口
 */
interface NoteRepository {
    /// 上传单张笔记图片，返回服务器 URL
    suspend fun uploadImage(uri: Uri): Result<String>
    /// 上传笔记视频，返回服务器 URL
    suspend fun uploadVideo(uri: Uri): Result<String>

    /// 发布图文笔记
    suspend fun publishNote(
        title: String?,
        content: String?,
        imageUrls: List<String>,
        coverSize: ImageSize?,
        status: Int = 1
    ): Result<Unit>

    /// 发布视频笔记
    suspend fun publishVideoNote(
        title: String?,
        content: String?,
        videoUrl: String,
        durationMs: Long,
        width: Int,
        height: Int,
        coverUrl: String?,
        coverWidth: Int? = null,
        coverHeight: Int? = null,
        status: Int = 1,
    ): Result<Unit>
    /// 发现页信息流
    fun discoverFeedPager(): Flow<PagingData<FeedItem>>
    /// 获取视频信息流
    suspend fun getVideoFeed(
        cursor: Long?,
        seedNoteId: Long?,
        orientation: String? = null,
        size: Int = 10,
    ): Result<FeedResponse>
    /// 获取指定用户已发布的笔记列表
    fun userNotesPager(userId: Long, noteType: Int? = null): Flow<PagingData<FeedItem>>
    /// 获取指定用户的点赞笔记列表
    fun userLikesPager(userId: Long, noteType: Int? = null): Flow<PagingData<FeedItem>>
    /// 获取指定用户的收藏笔记列表
    fun userFavoritesPager(userId: Long, noteType: Int? = null): Flow<PagingData<FeedItem>>
    /// 获取笔记详情
    suspend fun getNoteDetail(id: Long, noView: Boolean = false): Result<NoteDetailData>
    /// 点赞笔记
    suspend fun likeNote(id: Long): Result<Unit>
    /// 取消点赞笔记
    suspend fun unlikeNote(id: Long): Result<Unit>
    /// 收藏笔记
    suspend fun favoriteNote(id: Long): Result<Unit>
    /// 取消收藏笔记
    suspend fun unfavoriteNote(id: Long): Result<Unit>
    /// 获取浏览历史
    suspend fun getHistory(cursor: Long?, size: Int = 10): Result<HistoryResponse>
    /// 删除浏览历史条目
    suspend fun deleteHistory(ids: List<Long>): Result<Unit>
    /// 清空全部浏览历史
    suspend fun deleteHistoryAll(): Result<Unit>
}
