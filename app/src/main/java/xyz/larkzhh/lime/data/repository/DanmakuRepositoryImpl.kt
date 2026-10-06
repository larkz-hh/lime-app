package xyz.larkzhh.lime.data.repository

import xyz.larkzhh.lime.data.network.danmaku.DanmakuApi
import xyz.larkzhh.lime.data.network.model.DanmakuData
import xyz.larkzhh.lime.data.network.model.DanmakuListResponse
import xyz.larkzhh.lime.data.network.model.PostDanmakuRequest
import xyz.larkzhh.lime.domain.repository.DanmakuRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DanmakuRepositoryImpl @Inject constructor(
    private val apiService: DanmakuApi,
) : DanmakuRepository {

    /// 拉取弹幕列表
    override suspend fun getDanmaku(noteId: Long, fromMs: Long?, toMs: Long?): Result<DanmakuListResponse> = runCatching {
        val response = apiService.getDanmaku(noteId, fromMs, toMs)
        check(response.code == 200 && response.data != null) { response.message }
        response.data
    }

    /// 发弹幕
    override suspend fun postDanmaku(noteId: Long, content: String, videoTimeMs: Long, color: String?): Result<DanmakuData> = runCatching {
        val response = apiService.postDanmaku(noteId, PostDanmakuRequest(content, videoTimeMs, color))
        check(response.code == 200 && response.data != null) { response.message }
        response.data
    }

    /// 删除弹幕
    override suspend fun deleteDanmaku(noteId: Long, danmakuId: Long): Result<Unit> = runCatching {
        val response = apiService.deleteDanmaku(noteId, danmakuId)
        check(response.code == 200) { response.message }
    }
}
