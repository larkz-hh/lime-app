package xyz.larkzhh.lime.domain.repository

import android.net.Uri

/**
 *  笔记数据仓库接口
 */
interface NoteRepository {
    /// 上传单张笔记图片，返回服务器 URL
    suspend fun uploadImage(uri: Uri): Result<String>
    /// 发布图文笔记
    suspend fun publishNote(title: String?, content: String?, imageUrls: List<String>): Result<Unit>
}
