package xyz.larkzhh.lime.data.repository

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import xyz.larkzhh.lime.data.network.danmaku.DanmakuApi
import xyz.larkzhh.lime.data.network.model.ApiResponse
import xyz.larkzhh.lime.data.network.model.DanmakuData
import xyz.larkzhh.lime.data.network.model.DanmakuListResponse
import xyz.larkzhh.lime.data.network.model.FeedAuthor
import xyz.larkzhh.lime.data.network.model.PostDanmakuRequest

/// 弹幕仓库
class DanmakuRepositoryImplTest {

    private val api = FakeDanmakuApi()
    private val repository = DanmakuRepositoryImpl(api)

    @Test
    fun 拉取成功返回数据() = runBlocking {
        api.listResponse = ApiResponse(200, "ok", DanmakuListResponse(items = emptyList(), count = 0))

        val result = repository.getDanmaku(1L, null, null)

        assertEquals(0, result.getOrThrow().count)
    }

    @Test
    fun 拉取非200时失败并带上服务端消息() = runBlocking {
        api.listResponse = ApiResponse(403, "无权访问", null)

        val result = repository.getDanmaku(1L, null, null)

        assertTrue(result.isFailure)
        assertEquals("无权访问", result.exceptionOrNull()?.message)
    }

    @Test
    fun 拉取code为200但data为空也算失败() = runBlocking {
        api.listResponse = ApiResponse(200, "ok", null)

        assertTrue(repository.getDanmaku(1L, null, null).isFailure)
    }

    @Test
    fun 发弹幕成功返回新弹幕并透传请求体() = runBlocking {
        api.postResponse = ApiResponse(200, "ok", danmaku(7L))

        val result = repository.postDanmaku(1L, "哈哈", 1500L, "#fff")

        assertEquals(7L, result.getOrThrow().id)
        assertEquals("哈哈", api.lastPostRequest?.content)
        assertEquals(1500L, api.lastPostRequest?.videoTimeMs)
        assertEquals("#fff", api.lastPostRequest?.color)
    }

    @Test
    fun 发弹幕失败带上服务端消息() = runBlocking {
        api.postResponse = ApiResponse(500, "内容过长", null)

        val result = repository.postDanmaku(1L, "x", 0L, null)

        assertEquals("内容过长", result.exceptionOrNull()?.message)
    }

    @Test
    fun 删除只校验code不要求data() = runBlocking {
        api.deleteResponse = ApiResponse(200, "ok", null)

        assertTrue(repository.deleteDanmaku(1L, 2L).isSuccess)
    }

    @Test
    fun 删除非200时失败() = runBlocking {
        api.deleteResponse = ApiResponse(404, "弹幕不存在", null)

        val result = repository.deleteDanmaku(1L, 2L)

        assertTrue(result.isFailure)
        assertEquals("弹幕不存在", result.exceptionOrNull()?.message)
    }

    private fun danmaku(id: Long) = DanmakuData(
        id = id,
        content = "弹幕",
        videoTimeMs = 0L,
        author = FeedAuthor(id = 1L, nickname = "作者", avatar = null),
        createTime = "2026-10-04T10:00:00",
    )

    private class FakeDanmakuApi : DanmakuApi {
        var listResponse: ApiResponse<DanmakuListResponse> = ApiResponse(200, "ok", null)
        var postResponse: ApiResponse<DanmakuData> = ApiResponse(200, "ok", null)
        var deleteResponse: ApiResponse<Unit> = ApiResponse(200, "ok", null)
        var lastPostRequest: PostDanmakuRequest? = null

        override suspend fun getDanmaku(noteId: Long, fromMs: Long?, toMs: Long?): ApiResponse<DanmakuListResponse> =
            listResponse

        override suspend fun postDanmaku(noteId: Long, request: PostDanmakuRequest): ApiResponse<DanmakuData> {
            lastPostRequest = request
            return postResponse
        }

        override suspend fun deleteDanmaku(noteId: Long, danmakuId: Long): ApiResponse<Unit> = deleteResponse
    }
}
