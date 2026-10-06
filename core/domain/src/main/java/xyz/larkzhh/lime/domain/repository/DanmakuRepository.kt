package xyz.larkzhh.lime.domain.repository

import xyz.larkzhh.lime.data.network.model.DanmakuData
import xyz.larkzhh.lime.data.network.model.DanmakuListResponse

/// 弹幕仓库
interface DanmakuRepository {
    suspend fun getDanmaku(noteId: Long, fromMs: Long? = null, toMs: Long? = null): Result<DanmakuListResponse>
    suspend fun postDanmaku(noteId: Long, content: String, videoTimeMs: Long, color: String?): Result<DanmakuData>
    suspend fun deleteDanmaku(noteId: Long, danmakuId: Long): Result<Unit>
}
