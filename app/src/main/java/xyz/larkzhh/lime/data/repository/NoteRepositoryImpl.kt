package xyz.larkzhh.lime.data.repository

import android.content.Context
import android.net.Uri
import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okio.BufferedSink
import okio.source
import xyz.larkzhh.lime.data.local.feed.FeedLocalDataSource
import xyz.larkzhh.lime.data.local.note.NoteCacheLocalDataSource
import xyz.larkzhh.lime.data.network.model.FeedItem
import xyz.larkzhh.lime.data.network.model.FeedResponse
import xyz.larkzhh.lime.data.network.model.HistoryResponse
import xyz.larkzhh.lime.data.network.model.ImageSize
import xyz.larkzhh.lime.data.network.model.NoteDetailData
import xyz.larkzhh.lime.data.network.note.NoteRemoteDataSource
import xyz.larkzhh.lime.domain.repository.NoteRepository
import xyz.larkzhh.lime.util.ImageCompressor
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 笔记数据仓库实现
 */
@Singleton
class NoteRepositoryImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val feedLocalDataSource: FeedLocalDataSource,
    private val noteCacheLocalDataSource: NoteCacheLocalDataSource,
    private val noteRemoteDataSource: NoteRemoteDataSource,
) : NoteRepository {

    private val gson = Gson()

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

    /// 发现页信息流
    override fun discoverFeedPager(): Flow<PagingData<FeedItem>> =
        feedPager(FEED_KEY_DISCOVER) { cursor -> noteRemoteDataSource.getFeed(cursor, FEED_PAGE_SIZE) }

    /// 关注动态信息流
    override fun followingFeedPager(): Flow<PagingData<FeedItem>> =
        feedPager(FEED_KEY_FOLLOWING) { cursor ->
            noteRemoteDataSource.getFollowingFeed(cursor, FEED_PAGE_SIZE)
        }

    /// 获取指定用户已发布的笔记列表
    override fun userNotesPager(userId: Long, noteType: Int?): Flow<PagingData<FeedItem>> =
        feedPager(userFeedKey(userId, "notes", noteType)) { cursor ->
            noteRemoteDataSource.getUserNotes(userId, cursor, FEED_PAGE_SIZE).filterNotes(noteType)
        }

    /// 拉取页用户笔记
    override suspend fun fetchUserNotes(
        userId: Long,
        cursor: Long?,
        size: Int,
        status: String,
    ): Result<FeedResponse> =
        noteRemoteDataSource.getUserNotes(userId, cursor, size, status)

    /// 获取指定用户的点赞笔记列表
    override fun userLikesPager(userId: Long, noteType: Int?): Flow<PagingData<FeedItem>> =
        feedPager(userFeedKey(userId, "likes", noteType)) { cursor ->
            noteRemoteDataSource.getUserLikes(userId, cursor, FEED_PAGE_SIZE).filterNotes(noteType)
        }

    /// 获取指定用户的点赞笔记列表
    override fun userFavoritesPager(userId: Long, noteType: Int?): Flow<PagingData<FeedItem>> =
        feedPager(userFeedKey(userId, "favorites", noteType)) { cursor ->
            noteRemoteDataSource.getUserFavorites(userId, cursor, FEED_PAGE_SIZE).filterNotes(noteType)
        }

    /// 生成用户列表缓存键
    private fun userFeedKey(userId: Long, kind: String, noteType: Int?) =
        "user:$userId:$kind" + (noteType?.let { ":$it" } ?: "")

    /// 按笔记种类过滤一页结果
    private fun Result<FeedResponse>.filterNotes(noteType: Int?): Result<FeedResponse> =
        if (noteType == null) this
        else map { it.copy(items = it.items.filter { item -> item.noteType == noteType }) }

    /// 游标信息流构造
    @OptIn(ExperimentalPagingApi::class)
    private fun feedPager(
        feedKey: String,
        fetch: suspend (cursor: Long?) -> Result<FeedResponse>,
    ): Flow<PagingData<FeedItem>> =
        Pager(
            config = PagingConfig(
                pageSize = FEED_PAGE_SIZE,
                initialLoadSize = FEED_PAGE_SIZE * 2,
                enablePlaceholders = false,
            ),
            remoteMediator = FeedRemoteMediator(
                feedKey = feedKey,
                local = feedLocalDataSource,
                fetch = fetch,
            ),
            pagingSourceFactory = { feedLocalDataSource.pagingSource(feedKey) },
        ).flow.map { pagingData ->
            pagingData.map { gson.fromJson(it.json, FeedItem::class.java) }
        }

    /// 获取视频信息流
    override suspend fun getVideoFeed(
        cursor: Long?,
        seedNoteId: Long?,
        orientation: String?,
        size: Int,
    ): Result<FeedResponse> =
        noteRemoteDataSource.getVideoFeed(cursor, seedNoteId, orientation, size)

    /// 点赞笔记
    override suspend fun likeNote(id: Long): Result<Unit> = noteRemoteDataSource.likeNote(id)

    /// 取消点赞笔记
    override suspend fun unlikeNote(id: Long): Result<Unit> = noteRemoteDataSource.unlikeNote(id)

    /// 收藏笔记
    override suspend fun favoriteNote(id: Long): Result<Unit> = noteRemoteDataSource.favoriteNote(id)

    /// 取消收藏笔记
    override suspend fun unfavoriteNote(id: Long): Result<Unit> = noteRemoteDataSource.unfavoriteNote(id)

    /// 获取笔记详情
    override suspend fun getNoteDetail(id: Long, noView: Boolean): Result<NoteDetailData> {
        val result = noteRemoteDataSource.getNoteDetail(id, noView)
        result.getOrNull()?.let { note -> noteCacheLocalDataSource.saveNoteDetail(id, note) }
        return result
    }

    /// 读取本地缓存的笔记详情
    override suspend fun getCachedNoteDetail(id: Long): NoteDetailData? =
        noteCacheLocalDataSource.getNoteDetail(id)

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

    /// 编辑图文笔记
    override suspend fun updateNote(
        id: Long,
        title: String?,
        content: String?,
        imageUrls: List<String>,
        coverSize: ImageSize?,
        status: Int,
    ): Result<Unit> {
        val result = noteRemoteDataSource.updateNote(id, title, content, imageUrls, coverSize, status)
        if (result.isSuccess) refreshNoteCachesAfterEdit(id)// 同步本地缓存快照
        return result
    }

    /// 编辑视频笔记
    override suspend fun updateVideoNote(
        id: Long,
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
    ): Result<Unit> {
        val result = noteRemoteDataSource.updateVideoNote(
            id, title, content, videoUrl, durationMs, width, height,
            coverUrl, coverWidth, coverHeight, status,
        )
        if (result.isSuccess) refreshNoteCachesAfterEdit(id)// 同步本地缓存快照
        return result
    }

    /// 编辑保存成功后刷新本地缓存
    private suspend fun refreshNoteCachesAfterEdit(id: Long) {
        val detail = getNoteDetail(id, noView = true).getOrNull() ?: return
        runCatching {
            val cached = feedLocalDataSource.getNoteItems(id)
            if (cached.isEmpty()) return@runCatching
            val old = runCatching {
                gson.fromJson(cached.first().json, FeedItem::class.java)
            }.getOrNull()
            val cover = detail.images.firstOrNull()
            val feed = FeedItem(
                id = detail.id,
                title = detail.title,
                coverImage = if (detail.noteType == 2) detail.video?.coverUrl else cover?.url,
                coverWidth = cover?.width?.takeIf { it > 0 } ?: old?.coverWidth,
                coverHeight = cover?.height?.takeIf { it > 0 } ?: old?.coverHeight,
                likeCount = detail.likeCount,
                liked = detail.liked,
                author = detail.author,
                viewCount = old?.viewCount,
                noteType = detail.noteType,
                video = detail.video,
            )
            feedLocalDataSource.updateNoteItem(id, gson.toJson(feed))
        }
    }

    /// 删除笔记
    override suspend fun deleteNote(id: Long): Result<Unit> {
        val result = noteRemoteDataSource.deleteNote(id)
        result.onSuccess {
            runCatching {
                noteCacheLocalDataSource.deleteNoteCache(id)
                feedLocalDataSource.deleteByNoteId(id)
            }
        }
        return result
    }

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

    private companion object {
        const val FEED_PAGE_SIZE = 10
        const val FEED_KEY_DISCOVER = "discover"
        const val FEED_KEY_FOLLOWING = "following"
    }
}
