package xyz.larkzhh.lime.domain.repository

import android.net.Uri
import xyz.larkzhh.lime.data.network.model.FeedResponse
import xyz.larkzhh.lime.data.network.model.HistoryResponse
import xyz.larkzhh.lime.data.network.model.NoteDetailData

/**
 *  笔记数据仓库接口
 */
interface NoteRepository {
    /// 上传单张笔记图片，返回服务器 URL
    suspend fun uploadImage(uri: Uri): Result<String>
    /// 发布图文笔记
    suspend fun publishNote(title: String?, content: String?, imageUrls: List<String>, status: Int = 1): Result<Unit>
    /// 获取信息流，cursor 为空时从最新开始
    suspend fun getFeed(cursor: Long?, size: Int = 10): Result<FeedResponse>
    /// 获取指定用户已发布的笔记列表
    suspend fun getUserNotes(userId: Long, cursor: Long?, size: Int = 10): Result<FeedResponse>
    /// 同步读取指定用户已缓存的笔记首页
    fun getCachedUserNotes(userId: Long): FeedResponse?
    /// 获取指定用户的点赞笔记列表
    suspend fun getUserLikes(userId: Long, cursor: Long?, size: Int = 10): Result<FeedResponse>
    /// 获取指定用户的收藏笔记列表
    suspend fun getUserFavorites(userId: Long, cursor: Long?, size: Int = 10): Result<FeedResponse>
    /// 获取笔记详情
    suspend fun getNoteDetail(id: Long): Result<NoteDetailData>
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
