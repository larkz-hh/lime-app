package xyz.larkzhh.lime.domain.repository

interface AiRepository {

    /// AI 翻译
    suspend fun translate(text: String, targetLang: String, sourceLang: String? = null): Result<String>
}
