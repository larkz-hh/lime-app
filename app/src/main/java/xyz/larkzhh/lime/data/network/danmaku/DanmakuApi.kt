package xyz.larkzhh.lime.data.network.danmaku

import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import xyz.larkzhh.lime.data.network.model.ApiResponse
import xyz.larkzhh.lime.data.network.model.DanmakuData
import xyz.larkzhh.lime.data.network.model.DanmakuListResponse
import xyz.larkzhh.lime.data.network.model.PostDanmakuRequest

interface DanmakuApi {

    /// 发弹幕
    @POST("api/notes/{noteId}/danmaku")
    suspend fun postDanmaku(
        @Path("noteId") noteId: Long,
        @Body request: PostDanmakuRequest,
    ): ApiResponse<DanmakuData>

    /// 拉取弹幕列表
    @GET("api/notes/{noteId}/danmaku")
    suspend fun getDanmaku(@Path("noteId") noteId: Long): ApiResponse<DanmakuListResponse>

    /// 删除弹幕
    @DELETE("api/notes/{noteId}/danmaku/{danmakuId}")
    suspend fun deleteDanmaku(
        @Path("noteId") noteId: Long,
        @Path("danmakuId") danmakuId: Long,
    ): ApiResponse<Unit>
}
