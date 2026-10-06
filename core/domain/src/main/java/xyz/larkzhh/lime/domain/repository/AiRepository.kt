package xyz.larkzhh.lime.domain.repository

import kotlinx.coroutines.flow.Flow
import xyz.larkzhh.lime.domain.model.AiWriteEvent

interface AiRepository {

    /// AI 翻译
    suspend fun translate(text: String, targetLang: String, sourceLang: String? = null): Result<String>
    //// AI 写作辅助
    fun writeAssist(
        action: String,
        content: String? = null,
        imageUrls: List<String>? = null,
        model: String? = null,
    ): Flow<AiWriteEvent>
}
