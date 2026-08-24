package xyz.larkzhh.lime.data.repository

import xyz.larkzhh.lime.data.network.ApiService
import xyz.larkzhh.lime.data.network.model.AiTranslateRequest
import xyz.larkzhh.lime.domain.repository.AiRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AiRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
) : AiRepository {

    /// AI 翻译
    override suspend fun translate(
        text: String,
        targetLang: String,
        sourceLang: String?,
    ): Result<String> = runCatching {
        val response = apiService.aiTranslate(
            AiTranslateRequest(text = text, targetLang = targetLang, sourceLang = sourceLang)
        )
        check(response.code == 200 && response.data != null) { response.message }
        response.data.translatedText
    }
}
