package xyz.larkzhh.lime.data.network.search

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query
import xyz.larkzhh.lime.data.network.model.ApiResponse
import xyz.larkzhh.lime.data.network.model.HotSearchItem
import xyz.larkzhh.lime.data.network.model.NoteSearchResponse
import xyz.larkzhh.lime.data.network.model.SearchReportRequest
import xyz.larkzhh.lime.data.network.model.UserSearchResponse

interface SearchApi {

    /// 搜索笔记
    @GET("api/search/notes")
    suspend fun searchNotes(
        @Query("keyword") keyword: String,
        @Query("sort") sort: String,
        @Query("within") within: String,
        @Query("type") type: String,
        @Query("cursor") cursor: String?,
        @Query("size") size: Int,
    ): ApiResponse<NoteSearchResponse>

    /// 搜索用户
    @GET("api/search/users")
    suspend fun searchUsers(
        @Query("keyword") keyword: String,
        @Query("cursor") cursor: String?,
        @Query("size") size: Int,
    ): ApiResponse<UserSearchResponse>

    /// 搜索联想
    @GET("api/search/suggest")
    suspend fun getSearchSuggestions(
        @Query("q") q: String,
        @Query("size") size: Int,
    ): ApiResponse<List<String>>

    /// 热搜榜
    @GET("api/search/hot")
    suspend fun getHotSearches(
        @Query("size") size: Int,
    ): ApiResponse<List<HotSearchItem>>

    /// 上报搜索
    @POST("api/search/report")
    suspend fun reportSearch(@Body request: SearchReportRequest): ApiResponse<Unit>
}
