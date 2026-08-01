package xyz.larkzhh.lime.data.repository

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import xyz.larkzhh.lime.data.network.ApiService
import xyz.larkzhh.lime.data.network.model.DeleteHistoryRequest
import xyz.larkzhh.lime.data.network.model.FeedResponse
import xyz.larkzhh.lime.data.network.model.HistoryResponse
import xyz.larkzhh.lime.data.network.model.NoteDetailData
import xyz.larkzhh.lime.data.network.model.NoteImageRequest
import xyz.larkzhh.lime.data.network.model.PublishNoteRequest
import xyz.larkzhh.lime.domain.repository.NoteRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NoteRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
    @param:ApplicationContext private val context: Context,
) : NoteRepository {

    /// 上传笔记图片
    override suspend fun uploadImage(uri: Uri): Result<String> = runCatching {
        val bytes = context.contentResolver.openInputStream(uri)?.readBytes()
            ?: error("无法读取图片文件")
        val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
        val ext = when (mimeType) {
            "image/png" -> "png"
            "image/webp" -> "webp"
            "image/gif" -> "gif"
            else -> "jpg"
        }
        val requestBody = bytes.toRequestBody(mimeType.toMediaTypeOrNull())
        val part = MultipartBody.Part.createFormData("file", "upload.$ext", requestBody)
        val response = apiService.uploadNoteImage(part)
        check(response.code == 200 && response.data != null) { response.message }
        response.data.url
    }

    /// 获取信息流
    override suspend fun getFeed(cursor: Long?, size: Int): Result<FeedResponse> = runCatching {
        val response = apiService.getFeed(cursor = cursor, size = size)
        check(response.code == 200 && response.data != null) { response.message }
        response.data
    }

    /// 获取指定用户已发布的笔记列表
    override suspend fun getUserNotes(userId: Long, cursor: Long?, size: Int): Result<FeedResponse> = runCatching {
        val response = apiService.getUserNotes(userId = userId, cursor = cursor, size = size)
        check(response.code == 200 && response.data != null) { response.message }
        response.data
    }

    /// 获取指定用户的点赞笔记列表
    override suspend fun getUserLikes(userId: Long, cursor: Long?, size: Int): Result<FeedResponse> = runCatching {
        val response = apiService.getUserLikes(userId = userId, cursor = cursor, size = size)
        check(response.code == 200 && response.data != null) { response.message }
        response.data
    }

    /// 获取指定用户的收藏笔记列表
    override suspend fun getUserFavorites(userId: Long, cursor: Long?, size: Int): Result<FeedResponse> = runCatching {
        val response = apiService.getUserFavorites(userId = userId, cursor = cursor, size = size)
        check(response.code == 200 && response.data != null) { response.message }
        response.data
    }

    /// 点赞笔记
    override suspend fun likeNote(id: Long): Result<Unit> = runCatching {
        val response = apiService.likeNote(id)
        check(response.code == 200) { response.message }
    }

    /// 取消点赞笔记
    override suspend fun unlikeNote(id: Long): Result<Unit> = runCatching {
        val response = apiService.unlikeNote(id)
        check(response.code == 200) { response.message }
    }

    /// 收藏笔记
    override suspend fun favoriteNote(id: Long): Result<Unit> = runCatching {
        val response = apiService.favoriteNote(id)
        check(response.code == 200) { response.message }
    }

    /// 取消收藏笔记
    override suspend fun unfavoriteNote(id: Long): Result<Unit> = runCatching {
        val response = apiService.unfavoriteNote(id)
        check(response.code == 200) { response.message }
    }

    /// 获取笔记详情
    override suspend fun getNoteDetail(id: Long): Result<NoteDetailData> = runCatching {
        val response = apiService.getNoteDetail(id)
        check(response.code == 200 && response.data != null) { response.message }
        response.data
    }

    /// 获取浏览历史
    override suspend fun getHistory(cursor: Long?, size: Int): Result<HistoryResponse> = runCatching {
        val response = apiService.getHistory(cursor = cursor, size = size)
        check(response.code == 200 && response.data != null) { response.message }
        response.data
    }

    /// 删除浏览历史条目
    override suspend fun deleteHistory(ids: List<Long>): Result<Unit> = runCatching {
        val response = apiService.deleteHistory(DeleteHistoryRequest(noteIds = ids))
        check(response.code == 200) { response.message }
    }

    /// 清空全部浏览历史
    override suspend fun deleteHistoryAll(): Result<Unit> = runCatching {
        val response = apiService.deleteHistoryAll()
        check(response.code == 200) { response.message }
    }

    /// 发布笔记
    override suspend fun publishNote(
        title: String?,
        content: String?,
        imageUrls: List<String>,
        status: Int,
    ): Result<Unit> = runCatching {
        val images = imageUrls.mapIndexed { index, url ->
            NoteImageRequest(url = url, sortOrder = index)
        }
        val request = PublishNoteRequest(
            title = title?.ifBlank { null },
            content = content?.ifBlank { null },
            images = images,
            status = status,
        )
        val response = apiService.publishNote(request)
        check(response.code == 200) { response.message }
    }
}
