package xyz.larkzhh.lime.data.repository

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import xyz.larkzhh.lime.data.network.model.ApiResponse
import xyz.larkzhh.lime.data.network.model.HotSearchItem
import xyz.larkzhh.lime.data.network.model.NoteSearchResponse
import xyz.larkzhh.lime.data.network.model.SearchReportRequest
import xyz.larkzhh.lime.data.network.model.UserSearchResponse
import xyz.larkzhh.lime.data.network.search.SearchApi

/// 搜索仓库
class SearchRepositoryImplTest {

    private val api = FakeSearchApi()
    private val repository = SearchRepositoryImpl(api)

    @Test
    fun 搜索笔记成功返回数据() = runBlocking {
        api.notesResponse = ApiResponse(200, "ok", NoteSearchResponse(emptyList(), nextCursor = "c1", hasMore = true))

        val result = repository.searchNotes("旅行", "hot", "all", "all", null, 20)

        assertEquals("c1", result.getOrThrow().nextCursor)
    }

    @Test
    fun 搜索笔记非200时失败并带上服务端消息() = runBlocking {
        api.notesResponse = ApiResponse(400, "关键词不合法", null)

        val result = repository.searchNotes("旅行", "hot", "all", "all", null, 20)

        assertEquals("关键词不合法", result.exceptionOrNull()?.message)
    }

    @Test
    fun 搜索笔记code为200但data为空也算失败() = runBlocking {
        api.notesResponse = ApiResponse(200, "ok", null)

        assertTrue(repository.searchNotes("旅行", "hot", "all", "all", null, 20).isFailure)
    }

    @Test
    fun 搜索用户成功返回数据() = runBlocking {
        api.usersResponse = ApiResponse(200, "ok", UserSearchResponse(emptyList(), nextCursor = null, hasMore = false))

        assertTrue(repository.searchUsers("张三", null, 10).isSuccess)
    }

    @Test
    fun 搜索联想成功返回列表() = runBlocking {
        api.suggestResponse = ApiResponse(200, "ok", listOf("旅行", "旅行攻略"))

        assertEquals(listOf("旅行", "旅行攻略"), repository.getSuggestions("旅", 5).getOrThrow())
    }

    @Test
    fun 搜索联想data为空时失败() = runBlocking {
        api.suggestResponse = ApiResponse(200, "ok", null)

        assertTrue(repository.getSuggestions("旅", 5).isFailure)
    }

    @Test
    fun 热搜榜成功返回列表() = runBlocking {
        api.hotResponse = ApiResponse(200, "ok", listOf(HotSearchItem("旅行", 100)))

        assertEquals(1, repository.getHotSearches(10).getOrThrow().size)
    }

    @Test
    fun 上报搜索透传关键词() = runBlocking {
        repository.reportSearch("旅行")

        assertEquals("旅行", api.lastReportRequest?.keyword)
    }

    @Test
    fun 上报搜索不校验返回码() = runBlocking {
        // 与其余方法不同，reportSearch 只关心请求是否抛异常，不看 code
        api.reportResponse = ApiResponse(500, "服务异常", null)

        assertTrue(repository.reportSearch("旅行").isSuccess)
    }

    private class FakeSearchApi : SearchApi {
        var notesResponse: ApiResponse<NoteSearchResponse> = ApiResponse(200, "ok", null)
        var usersResponse: ApiResponse<UserSearchResponse> = ApiResponse(200, "ok", null)
        var suggestResponse: ApiResponse<List<String>> = ApiResponse(200, "ok", null)
        var hotResponse: ApiResponse<List<HotSearchItem>> = ApiResponse(200, "ok", null)
        var reportResponse: ApiResponse<Unit> = ApiResponse(200, "ok", null)
        var lastReportRequest: SearchReportRequest? = null

        override suspend fun searchNotes(
            keyword: String,
            sort: String,
            within: String,
            type: String,
            cursor: String?,
            size: Int,
        ): ApiResponse<NoteSearchResponse> = notesResponse

        override suspend fun searchUsers(keyword: String, cursor: String?, size: Int): ApiResponse<UserSearchResponse> =
            usersResponse

        override suspend fun getSearchSuggestions(q: String, size: Int): ApiResponse<List<String>> = suggestResponse

        override suspend fun getHotSearches(size: Int): ApiResponse<List<HotSearchItem>> = hotResponse

        override suspend fun reportSearch(request: SearchReportRequest): ApiResponse<Unit> {
            lastReportRequest = request
            return reportResponse
        }
    }
}
