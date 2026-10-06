package xyz.larkzhh.lime.data.network.note

import kotlinx.coroutines.runBlocking
import okhttp3.MultipartBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import xyz.larkzhh.lime.data.network.model.ApiResponse
import xyz.larkzhh.lime.data.network.model.DeleteHistoryRequest
import xyz.larkzhh.lime.data.network.model.FeedResponse
import xyz.larkzhh.lime.data.network.model.HistoryResponse
import xyz.larkzhh.lime.data.network.model.ImageSize
import xyz.larkzhh.lime.data.network.model.NoteData
import xyz.larkzhh.lime.data.network.model.NoteDetailData
import xyz.larkzhh.lime.data.network.model.PublishNoteRequest
import xyz.larkzhh.lime.data.network.model.PublishVideoNoteRequest
import xyz.larkzhh.lime.data.network.model.UploadNoteImageResponse

/// 笔记远端数据源的请求组装
class NoteRemoteDataSourceTest {

    private val api = FakeNoteApi()
    private val dataSource = NoteRemoteDataSource(api)

    @Test
    fun 第一张图作为封面带上尺寸() = runBlocking {
        dataSource.publishNote(
            title = "标题",
            content = "正文",
            imageUrls = listOf("a.jpg", "b.jpg", "c.jpg"),
            coverSize = ImageSize(1080, 1440),
            status = 1,
        )

        val images = api.lastPublishRequest.orFail().images
        assertEquals(1080, images[0].width)
        assertEquals(1440, images[0].height)
        assertNull(images[1].width)
        assertNull(images[1].height)
        assertNull(images[2].width)
        assertNull(images[2].height)
    }

    @Test
    fun 图片按顺序编号() = runBlocking {
        dataSource.publishNote("标题", "正文", listOf("a.jpg", "b.jpg", "c.jpg"), null, 1)

        val images = api.lastPublishRequest.orFail().images
        assertEquals(listOf("a.jpg", "b.jpg", "c.jpg"), images.map { it.url })
        assertEquals(listOf(0, 1, 2), images.map { it.sortOrder })
    }

    @Test
    fun 没有封面尺寸时首图尺寸为空() = runBlocking {
        dataSource.publishNote("标题", "正文", listOf("a.jpg"), null, 1)

        val images = api.lastPublishRequest.orFail().images
        assertNull(images[0].width)
        assertNull(images[0].height)
    }

    @Test
    fun 空白标题与正文转成null() = runBlocking {
        dataSource.publishNote("   ", "", listOf("a.jpg"), null, 1)

        val request = api.lastPublishRequest.orFail()
        assertNull(request.title)
        assertNull(request.content)
    }

    @Test
    fun 正常标题与正文原样保留() = runBlocking {
        dataSource.publishNote("标题", "正文", listOf("a.jpg"), null, 1)

        val request = api.lastPublishRequest.orFail()
        assertEquals("标题", request.title)
        assertEquals("正文", request.content)
    }

    @Test
    fun 编辑图文笔记的图片映射与发布一致() = runBlocking {
        dataSource.updateNote(
            id = 7L,
            title = "标题",
            content = "正文",
            imageUrls = listOf("a.jpg", "b.jpg"),
            coverSize = ImageSize(800, 600),
            status = 0,
        )

        assertEquals(7L, api.lastUpdateId)
        val images = api.lastUpdateRequest.orFail().images
        assertEquals(800, images[0].width)
        assertEquals(600, images[0].height)
        assertNull(images[1].width)
        assertEquals(0, api.lastUpdateRequest.orFail().status)
    }

    @Test
    fun 发布视频笔记组装视频字段() = runBlocking {
        dataSource.publishVideoNote(
            title = "视频",
            content = "   ",
            videoUrl = "v.mp4",
            durationMs = 12_000L,
            width = 1920,
            height = 1080,
            coverUrl = "cover.jpg",
            coverWidth = 640,
            coverHeight = 360,
            status = 1,
        )

        val video = api.lastPublishVideoRequest.orFail().video
        assertEquals("v.mp4", video.url)
        assertEquals(12_000L, video.durationMs)
        assertEquals(1920, video.width)
        assertEquals(1080, video.height)
        assertEquals("cover.jpg", video.coverUrl)
        assertEquals(640, video.coverWidth)
        assertEquals(360, video.coverHeight)
        assertNull(api.lastPublishVideoRequest.orFail().content)
    }

    @Test
    fun 发布非200时失败并带上服务端消息() = runBlocking {
        api.publishResponse = ApiResponse(400, "标题过长", null)

        val result = dataSource.publishNote("标题", "正文", listOf("a.jpg"), null, 1)

        assertTrue(result.isFailure)
        assertEquals("标题过长", result.exceptionOrNull()?.message)
    }

    private fun PublishNoteRequest?.orFail(): PublishNoteRequest = requireNotNull(this) { "未捕获到请求体" }

    private fun PublishVideoNoteRequest?.orFail(): PublishVideoNoteRequest = requireNotNull(this) { "未捕获到请求体" }

    private class FakeNoteApi : NoteApi {
        var publishResponse: ApiResponse<NoteData> = ApiResponse(200, "ok", null)
        var lastPublishRequest: PublishNoteRequest? = null
        var lastUpdateRequest: PublishNoteRequest? = null
        var lastUpdateId: Long? = null
        var lastPublishVideoRequest: PublishVideoNoteRequest? = null

        override suspend fun publishNote(request: PublishNoteRequest): ApiResponse<NoteData> {
            lastPublishRequest = request
            return publishResponse
        }

        override suspend fun updateNote(id: Long, request: PublishNoteRequest): ApiResponse<NoteData> {
            lastUpdateId = id
            lastUpdateRequest = request
            return publishResponse
        }

        override suspend fun publishVideoNote(request: PublishVideoNoteRequest): ApiResponse<NoteData> {
            lastPublishVideoRequest = request
            return publishResponse
        }

        override suspend fun updateVideoNote(id: Long, request: PublishVideoNoteRequest): ApiResponse<NoteData> =
            publishResponse

        override suspend fun uploadNoteImage(file: MultipartBody.Part): ApiResponse<UploadNoteImageResponse> =
            error("未使用")

        override suspend fun uploadNoteVideo(file: MultipartBody.Part): ApiResponse<UploadNoteImageResponse> =
            error("未使用")

        override suspend fun deleteNote(id: Long): ApiResponse<Unit> = error("未使用")

        override suspend fun likeNote(id: Long): ApiResponse<Unit> = error("未使用")

        override suspend fun unlikeNote(id: Long): ApiResponse<Unit> = error("未使用")

        override suspend fun getNoteDetail(id: Long, noView: Boolean): ApiResponse<NoteDetailData> = error("未使用")

        override suspend fun favoriteNote(id: Long): ApiResponse<Unit> = error("未使用")

        override suspend fun unfavoriteNote(id: Long): ApiResponse<Unit> = error("未使用")

        override suspend fun getFeed(cursor: Long?, size: Int): ApiResponse<FeedResponse> = error("未使用")

        override suspend fun getFollowingFeed(cursor: Long?, size: Int): ApiResponse<FeedResponse> = error("未使用")

        override suspend fun getVideoFeed(
            cursor: Long?,
            seedNoteId: Long?,
            orientation: String?,
            size: Int,
        ): ApiResponse<FeedResponse> = error("未使用")

        override suspend fun getUserNotes(
            userId: Long,
            status: String,
            cursor: Long?,
            size: Int,
        ): ApiResponse<FeedResponse> = error("未使用")

        override suspend fun getUserLikes(userId: Long, cursor: Long?, size: Int): ApiResponse<FeedResponse> =
            error("未使用")

        override suspend fun getUserFavorites(userId: Long, cursor: Long?, size: Int): ApiResponse<FeedResponse> =
            error("未使用")

        override suspend fun getHistory(cursor: Long?, size: Int): ApiResponse<HistoryResponse> = error("未使用")

        override suspend fun deleteHistory(request: DeleteHistoryRequest): ApiResponse<Unit> = error("未使用")

        override suspend fun deleteHistoryAll(): ApiResponse<Unit> = error("未使用")
    }
}
