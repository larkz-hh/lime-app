package xyz.larkzhh.lime.data.repository

import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.OkHttpClient
import xyz.larkzhh.lime.data.network.ai.AiApi
import xyz.larkzhh.lime.data.network.collectSse
import xyz.larkzhh.lime.data.network.model.AiTranslateRequest
import xyz.larkzhh.lime.data.network.model.AiWriteAssistRequest
import xyz.larkzhh.lime.domain.model.AiWriteEvent
import xyz.larkzhh.lime.domain.repository.AiRepository
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class AiRepositoryImpl @Inject constructor(
    private val apiService: AiApi,
    @Named("sse") private val sseClient: OkHttpClient,
    @Named("base_url") private val baseUrl: String,
    private val gson: Gson,
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
        val data = response.data
        check(response.code == 200 && data != null) { response.message }
        data.translatedText.stripDataTags()
    }

    private fun String.stripDataTags(): String =
        replace("<data>", "").replace("</data>", "").trim()

    /// AI 写作辅助
    override fun writeAssist(
        action: String,
        content: String?,
        imageUrls: List<String>?,
        model: String?,
    ): Flow<AiWriteEvent> = flow {
        collectSse(
            client = sseClient,
            url = "${baseUrl}api/ai/write/assist",
            jsonBody = gson.toJson(AiWriteAssistRequest(action, content, imageUrls, model)),
            gson = gson,
        ) { dto ->
            when (dto.type) {
                "delta" -> dto.content?.let { AiWriteEvent.Delta(it) }
                "done" -> AiWriteEvent.Done(dto.content.orEmpty(), dto.model)
                "error" -> AiWriteEvent.Error(dto.message ?: "AI 服务暂时不可用")
                else -> null
            }
        }
    }.flowOn(Dispatchers.IO)
}
