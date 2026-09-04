package xyz.larkzhh.lime.data.network.note

import okhttp3.MultipartBody
import xyz.larkzhh.lime.data.network.ApiService
import xyz.larkzhh.lime.data.network.model.DeleteHistoryRequest
import xyz.larkzhh.lime.data.network.model.FeedResponse
import xyz.larkzhh.lime.data.network.model.HistoryResponse
import xyz.larkzhh.lime.data.network.model.ImageSize
import xyz.larkzhh.lime.data.network.model.NoteDetailData
import xyz.larkzhh.lime.data.network.model.NoteImageRequest
import xyz.larkzhh.lime.data.network.model.PublishNoteRequest
import xyz.larkzhh.lime.data.network.model.PublishVideoNoteRequest
import xyz.larkzhh.lime.data.network.model.VideoRequest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 笔记模块远端数据源
 */
@Singleton
class NoteRemoteDataSource @Inject constructor(
    private val apiService: ApiService,
) {

    /// 上传笔记图片
    suspend fun uploadNoteImage(part: MultipartBody.Part): Result<String> = runCatching {
        val response = apiService.uploadNoteImage(part)
        check(response.code == 200 && response.data != null) { response.message }
        response.data.url
    }

    /// 上传笔记视频
    suspend fun uploadNoteVideo(part: MultipartBody.Part): Result<String> = runCatching {
        val response = apiService.uploadNoteVideo(part)
        check(response.code == 200 && response.data != null) { response.message }
        response.data.url
    }

    /// 获取信息流
    suspend fun getFeed(cursor: Long?, size: Int): Result<FeedResponse> = runCatching {
        val response = apiService.getFeed(cursor = cursor, size = size)
        check(response.code == 200 && response.data != null) { response.message }
        response.data
    }

    /// 获取关注动态
    suspend fun getFollowingFeed(cursor: Long?, size: Int): Result<FeedResponse> = runCatching {
        val response = apiService.getFollowingFeed(cursor = cursor, size = size)
        check(response.code == 200 && response.data != null) { response.message }
        response.data
    }

    /// 获取视频信息流
    suspend fun getVideoFeed(
        cursor: Long?,
        seedNoteId: Long?,
        orientation: String?,
        size: Int,
    ): Result<FeedResponse> = runCatching {
        val response = apiService.getVideoFeed(
            cursor = cursor,
            seedNoteId = seedNoteId,
            orientation = orientation,
            size = size,
        )
        check(response.code == 200 && response.data != null) { response.message }
        response.data
    }

    /// 获取指定用户已发布的笔记列表
    suspend fun getUserNotes(userId: Long, cursor: Long?, size: Int): Result<FeedResponse> = runCatching {
        val response = apiService.getUserNotes(userId = userId, cursor = cursor, size = size)
        check(response.code == 200 && response.data != null) { response.message }
        response.data
    }

    /// 获取指定用户的点赞笔记列表
    suspend fun getUserLikes(userId: Long, cursor: Long?, size: Int): Result<FeedResponse> = runCatching {
        val response = apiService.getUserLikes(userId = userId, cursor = cursor, size = size)
        check(response.code == 200 && response.data != null) { response.message }
        response.data
    }

    /// 获取指定用户的收藏笔记列表
    suspend fun getUserFavorites(userId: Long, cursor: Long?, size: Int): Result<FeedResponse> = runCatching {
        val response = apiService.getUserFavorites(userId = userId, cursor = cursor, size = size)
        check(response.code == 200 && response.data != null) { response.message }
        response.data
    }

    /// 点赞笔记
    suspend fun likeNote(id: Long): Result<Unit> = runCatching {
        val response = apiService.likeNote(id)
        check(response.code == 200) { response.message }
    }

    /// 取消点赞笔记
    suspend fun unlikeNote(id: Long): Result<Unit> = runCatching {
        val response = apiService.unlikeNote(id)
        check(response.code == 200) { response.message }
    }

    /// 收藏笔记
    suspend fun favoriteNote(id: Long): Result<Unit> = runCatching {
        val response = apiService.favoriteNote(id)
        check(response.code == 200) { response.message }
    }

    /// 取消收藏笔记
    suspend fun unfavoriteNote(id: Long): Result<Unit> = runCatching {
        val response = apiService.unfavoriteNote(id)
        check(response.code == 200) { response.message }
    }

    /// 获取笔记详情
    suspend fun getNoteDetail(id: Long, noView: Boolean): Result<NoteDetailData> = runCatching {
        val response = apiService.getNoteDetail(id, noView = noView)
        check(response.code == 200 && response.data != null) { response.message }
        response.data
    }

    /// 获取浏览历史
    suspend fun getHistory(cursor: Long?, size: Int): Result<HistoryResponse> = runCatching {
        val response = apiService.getHistory(cursor = cursor, size = size)
        check(response.code == 200 && response.data != null) { response.message }
        response.data
    }

    /// 删除浏览历史条目
    suspend fun deleteHistory(ids: List<Long>): Result<Unit> = runCatching {
        val response = apiService.deleteHistory(DeleteHistoryRequest(noteIds = ids))
        check(response.code == 200) { response.message }
    }

    /// 清空全部浏览历史
    suspend fun deleteHistoryAll(): Result<Unit> = runCatching {
        val response = apiService.deleteHistoryAll()
        check(response.code == 200) { response.message }
    }

    /// 发布图文笔记
    suspend fun publishNote(
        title: String?,
        content: String?,
        imageUrls: List<String>,
        coverSize: ImageSize?,
        status: Int,
    ): Result<Unit> = runCatching {
        val images = imageUrls.mapIndexed { index, url ->
            val isCover = index == 0
            NoteImageRequest(
                url = url,
                sortOrder = index,
                width = if (isCover) coverSize?.width else null,
                height = if (isCover) coverSize?.height else null,
            )
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

    /// 发布视频笔记
    suspend fun publishVideoNote(
        title: String?,
        content: String?,
        videoUrl: String,
        durationMs: Long,
        width: Int,
        height: Int,
        coverUrl: String?,
        coverWidth: Int?,
        coverHeight: Int?,
        status: Int,
    ): Result<Unit> = runCatching {
        val request = PublishVideoNoteRequest(
            title = title?.ifBlank { null },
            content = content?.ifBlank { null },
            video = VideoRequest(
                url = videoUrl,
                durationMs = durationMs,
                width = width,
                height = height,
                coverUrl = coverUrl,
                coverWidth = coverWidth,
                coverHeight = coverHeight,
            ),
            status = status,
        )
        val response = apiService.publishVideoNote(request)
        check(response.code == 200) { response.message }
    }
}
