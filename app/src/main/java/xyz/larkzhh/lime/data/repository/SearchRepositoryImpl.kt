package xyz.larkzhh.lime.data.repository

import xyz.larkzhh.lime.data.network.ApiService
import xyz.larkzhh.lime.data.network.model.HotSearchItem
import xyz.larkzhh.lime.data.network.model.NoteSearchResponse
import xyz.larkzhh.lime.data.network.model.SearchReportRequest
import xyz.larkzhh.lime.domain.repository.SearchRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SearchRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
) : SearchRepository {

    /// 搜索笔记
    override suspend fun searchNotes(
        keyword: String,
        sort: String,
        within: String,
        cursor: String?,
        size: Int,
    ): Result<NoteSearchResponse> = runCatching {
        val response = apiService.searchNotes(
            keyword = keyword,
            sort = sort,
            within = within,
            cursor = cursor,
            size = size,
        )
        check(response.code == 200 && response.data != null) { response.message }
        response.data
    }

    /// 搜索联想
    override suspend fun getSuggestions(q: String, size: Int): Result<List<String>> = runCatching {
        val response = apiService.getSearchSuggestions(q = q, size = size)
        check(response.code == 200 && response.data != null) { response.message }
        response.data
    }

    /// 热搜榜
    override suspend fun getHotSearches(size: Int): Result<List<HotSearchItem>> = runCatching {
        val response = apiService.getHotSearches(size = size)
        check(response.code == 200 && response.data != null) { response.message }
        response.data
    }

    /// 上报搜索
    override suspend fun reportSearch(keyword: String): Result<Unit> = runCatching {
        apiService.reportSearch(SearchReportRequest(keyword))
    }
}
