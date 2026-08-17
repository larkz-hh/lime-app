package xyz.larkzhh.lime.domain.repository

import xyz.larkzhh.lime.data.network.model.HotSearchItem
import xyz.larkzhh.lime.data.network.model.NoteSearchResponse
import xyz.larkzhh.lime.data.network.model.UserSearchResponse

/**
 * 搜索仓库接口
 */
interface SearchRepository {

    /// 搜索笔记
    suspend fun searchNotes(
        keyword: String,
        sort: String = "composite",
        within: String = "all",
        cursor: String? = null,
        size: Int = 10,
    ): Result<NoteSearchResponse>

    /// 搜索用户
    suspend fun searchUsers(
        keyword: String,
        cursor: String? = null,
        size: Int = 10,
    ): Result<UserSearchResponse>

    /// 搜索联想
    suspend fun getSuggestions(q: String, size: Int = 15): Result<List<String>>

    /// 热搜榜
    suspend fun getHotSearches(size: Int = 10): Result<List<HotSearchItem>>

    /// 上报搜索，热搜统计
    suspend fun reportSearch(keyword: String): Result<Unit>
}
