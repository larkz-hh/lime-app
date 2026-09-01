package xyz.larkzhh.lime.data.repository

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okio.BufferedSink
import okio.source
import xyz.larkzhh.lime.data.local.feed.FeedLocalDataSource
import xyz.larkzhh.lime.data.network.model.FeedResponse
import xyz.larkzhh.lime.data.network.model.HistoryResponse
import xyz.larkzhh.lime.data.network.model.ImageSize
import xyz.larkzhh.lime.data.network.model.NoteDetailData
import xyz.larkzhh.lime.data.network.note.NoteRemoteDataSource
import xyz.larkzhh.lime.domain.repository.NoteRepository
import xyz.larkzhh.lime.util.ImageCompressor
import xyz.larkzhh.lime.util.LruCache
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NoteRepositoryImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val feedLocalDataSource: FeedLocalDataSource,
    private val noteRemoteDataSource: NoteRemoteDataSource,
) : NoteRepository {

    private val userNotesFirstPageCache = LruCache<Long, FeedResponse>(maxSize = 50)

    /// 上传笔记图片
    override suspend fun uploadImage(uri: Uri): Result<String> {
        val image = runCatching { ImageCompressor.compress(context, uri) }
            .getOrElse { return Result.failure(it) }
        val requestBody = image.bytes.toRequestBody(image.mimeType.toMediaTypeOrNull())
        val part = MultipartBody.Part.createFormData("file", "upload.${image.ext}", requestBody)
        return noteRemoteDataSource.uploadNoteImage(part)
    }

    /// 上传笔记视频，流式写入
    override suspend fun uploadVideo(uri: Uri): Result<String> {
        val mimeType = context.contentResolver.getType(uri) ?: "video/mp4"
        val requestBody = uri.asStreamingRequestBody(mimeType.toMediaTypeOrNull())
        val part = MultipartBody.Part.createFormData("file", "upload.mp4", requestBody)
        return noteRemoteDataSource.uploadNoteVideo(part)
    }

    /// 将内容 uri 包装为流式 RequestBody
    private fun Uri.asStreamingRequestBody(contentType: MediaType?): RequestBody =
        object : RequestBody() {
            override fun contentType(): MediaType? = contentType// 声明请求体的内容类型

            override fun contentLength(): Long =
                context.contentResolver.openFileDescriptor(this@asStreamingRequestBody, "r")
                    ?.use { it.statSize } ?: -1L// 获取文件的总大小

            override fun writeTo(sink: BufferedSink) {
                val stream = context.contentResolver.openInputStream(this@asStreamingRequestBody)
                    ?: error("无法读取视频文件")
                stream.source().use { source -> sink.writeAll(source) }
            }// 流式写入
        }

    /// 获取信息流
    override suspend fun getFeed(cursor: Long?, size: Int): Result<FeedResponse> {
        val result = noteRemoteDataSource.getFeed(cursor, size)
        if (cursor == null) result.getOrNull()?.let { feedLocalDataSource.saveFirstPage(it) }
        return result
    }

    /// 同步读取本地缓存的首页信息流
    override suspend fun getCachedFeedFirstPage(): FeedResponse? =
        feedLocalDataSource.getFirstPage()

    /// 获取视频信息流
    override suspend fun getVideoFeed(
        cursor: Long?,
        seedNoteId: Long?,
        orientation: String?,
        size: Int,
    ): Result<FeedResponse> =
        noteRemoteDataSource.getVideoFeed(cursor, seedNoteId, orientation, size)

    /// 获取指定用户已发布的笔记列表
    override suspend fun getUserNotes(userId: Long, cursor: Long?, size: Int): Result<FeedResponse> {
        val result = noteRemoteDataSource.getUserNotes(userId, cursor, size)
        if (cursor == null) result.getOrNull()?.let { userNotesFirstPageCache[userId] = it }
        return result
    }

    /// 同步读取指定用户缓存的笔记首页
    override fun getCachedUserNotes(userId: Long): FeedResponse? = userNotesFirstPageCache[userId]

    /// 获取指定用户的点赞笔记列表
    override suspend fun getUserLikes(userId: Long, cursor: Long?, size: Int): Result<FeedResponse> =
        noteRemoteDataSource.getUserLikes(userId, cursor, size)

    /// 获取指定用户的收藏笔记列表
    override suspend fun getUserFavorites(userId: Long, cursor: Long?, size: Int): Result<FeedResponse> =
        noteRemoteDataSource.getUserFavorites(userId, cursor, size)

    /// 点赞笔记
    override suspend fun likeNote(id: Long): Result<Unit> = noteRemoteDataSource.likeNote(id)

    /// 取消点赞笔记
    override suspend fun unlikeNote(id: Long): Result<Unit> = noteRemoteDataSource.unlikeNote(id)

    /// 收藏笔记
    override suspend fun favoriteNote(id: Long): Result<Unit> = noteRemoteDataSource.favoriteNote(id)

    /// 取消收藏笔记
    override suspend fun unfavoriteNote(id: Long): Result<Unit> = noteRemoteDataSource.unfavoriteNote(id)

    /// 获取笔记详情
    override suspend fun getNoteDetail(id: Long, noView: Boolean): Result<NoteDetailData> =
        noteRemoteDataSource.getNoteDetail(id, noView)

    /// 获取浏览历史
    override suspend fun getHistory(cursor: Long?, size: Int): Result<HistoryResponse> =
        noteRemoteDataSource.getHistory(cursor, size)

    /// 删除浏览历史条目
    override suspend fun deleteHistory(ids: List<Long>): Result<Unit> =
        noteRemoteDataSource.deleteHistory(ids)

    /// 清空全部浏览历史
    override suspend fun deleteHistoryAll(): Result<Unit> = noteRemoteDataSource.deleteHistoryAll()

    /// 发布笔记
    override suspend fun publishNote(
        title: String?,
        content: String?,
        imageUrls: List<String>,
        coverSize: ImageSize?,
        status: Int,
    ): Result<Unit> =
        noteRemoteDataSource.publishNote(title, content, imageUrls, coverSize, status)

    /// 发布视频笔记
    override suspend fun publishVideoNote(
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
    ): Result<Unit> =
        noteRemoteDataSource.publishVideoNote(
            title, content, videoUrl, durationMs, width, height,
            coverUrl, coverWidth, coverHeight, status,
        )
}
