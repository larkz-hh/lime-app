package xyz.larkzhh.lime.data.repository

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.runInterruptible
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import xyz.larkzhh.lime.data.network.ApiService
import xyz.larkzhh.lime.data.network.model.AiTranslateRequest
import xyz.larkzhh.lime.data.network.model.AiWriteAssistRequest
import xyz.larkzhh.lime.data.network.model.AiWriteEventDto
import xyz.larkzhh.lime.data.network.model.ApiResponse
import xyz.larkzhh.lime.domain.model.AiWriteEvent
import xyz.larkzhh.lime.domain.repository.AiRepository
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class AiRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
    @Named("sse") private val sseClient: OkHttpClient,
    @Named("base_url") private val baseUrl: String,
) : AiRepository {

    private val gson = Gson()

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

    /// AI 写作辅助
    override fun writeAssist(
        action: String,
        content: String?,
        imageUrls: List<String>?,
        model: String?,
    ): Flow<AiWriteEvent> = flow {
        val request = Request.Builder()
            .url("${baseUrl}api/ai/write/assist")
            .header("Accept", "text/event-stream")
            .post(
                gson.toJson(AiWriteAssistRequest(action, content, imageUrls, model))
                    .toRequestBody("application/json; charset=utf-8".toMediaType())
            )
            .build()

        val response = runInterruptible(Dispatchers.IO) { sseClient.newCall(request).execute() }
        response.use {
            // http 层错误检查
            if (!it.isSuccessful) {
                emit(AiWriteEvent.Error(parseErrorMessage(it.body?.string())))
                return@flow
            }
            // 业务错误检查
            val contentType = it.header("Content-Type").orEmpty()
            if (!contentType.contains("text/event-stream")) {
                emit(AiWriteEvent.Error(parseErrorMessage(it.body?.string())))
                return@flow
            }
            val source = it.body?.source() ?: run {
                emit(AiWriteEvent.Error("AI 服务暂时不可用"))
                return@flow
            }
            var terminated = false
            while (!terminated) {
                val line = runInterruptible(Dispatchers.IO) { source.readUtf8Line() } ?: break
                if (line.startsWith("{")) {
                    emit(AiWriteEvent.Error(parseErrorMessage(line)))
                    terminated = true
                    continue
                }
                if (!line.startsWith("data:")) continue
                val payload = line.removePrefix("data:").trim()
                if (payload.isEmpty() || payload == "[DONE]") continue
                val event = runCatching { gson.fromJson(payload, AiWriteEventDto::class.java) }
                    .getOrNull() ?: continue
                when (event.type) {
                    "delta" -> event.content?.let { emit(AiWriteEvent.Delta(it)) }
                    "done" -> {
                        emit(AiWriteEvent.Done(event.content.orEmpty(), event.model))
                        terminated = true
                    }
                    "error" -> {
                        emit(AiWriteEvent.Error(event.message ?: "AI 服务暂时不可用"))
                        terminated = true
                    }
                }
            }
            if (!terminated) emit(AiWriteEvent.Error("AI 服务暂时不可用"))
        }
    }.flowOn(Dispatchers.IO)

    /// 解析 http 错误响应体
    private fun parseErrorMessage(body: String?): String {
        if (body.isNullOrBlank()) return "AI 服务暂时不可用"
        return runCatching {
            val type = object : TypeToken<ApiResponse<Any?>>() {}.type
            val parsed: ApiResponse<Any?> = gson.fromJson(body, type)
            parsed.message.ifBlank { "AI 服务暂时不可用" }
        }.getOrDefault("AI 服务暂时不可用")
    }
}
