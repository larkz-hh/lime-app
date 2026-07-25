package xyz.larkzhh.lime.domain.repository

import android.net.Uri
import xyz.larkzhh.lime.data.network.model.FeedResponse

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
}
