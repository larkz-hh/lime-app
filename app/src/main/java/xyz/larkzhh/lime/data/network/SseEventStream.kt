package xyz.larkzhh.lime.data.network

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.runInterruptible
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import xyz.larkzhh.lime.data.network.model.ApiResponse
import java.io.IOException

/**
 * 通用 AI SSE 事件 DTO
 */
data class AiSseEventDto(
    val type: String? = null,// delta、done、error
    val content: String? = null,
    val message: String? = null,
    val model: String? = null,
    val userMessageId: Long? = null,
    val assistantMessageId: Long? = null,
)

private const val NETWORK_ERROR_MESSAGE = "网络连接中断，请重试"

/**
 * 通用 SSE 流式请求工具
 */
suspend fun <T> FlowCollector<T>.collectSse(
    client: OkHttpClient,
    url: String,
    jsonBody: String,
    parseEvent: suspend (AiSseEventDto) -> T?,
) {
    val gson = Gson()
    val request = Request.Builder()
        .url(url)
        .header("Accept", "text/event-stream")
        .post(jsonBody.toRequestBody("application/json; charset=utf-8".toMediaType()))
        .build()

    // 执行请求
    val response = try {
        runInterruptible(Dispatchers.IO) { client.newCall(request).execute() }
    } catch (ce: CancellationException) {
        throw ce
    } catch (e: IOException) {
        parseEvent(AiSseEventDto(type = "error", message = NETWORK_ERROR_MESSAGE))?.let { emit(it) }
        return
    }

    var ended = false
    try {
        response.use { resp ->
            // http 层错误
            if (!resp.isSuccessful) {
                val message = parseErrorMessage(resp.body?.string())
                parseEvent(AiSseEventDto(type = "error", message = message))?.let { emit(it) }
                return@use
            }
            // 非 SSE
            val contentType = resp.header("Content-Type").orEmpty()
            if (!contentType.contains("text/event-stream")) {
                val message = parseErrorMessage(resp.body?.string())
                parseEvent(AiSseEventDto(type = "error", message = message))?.let { emit(it) }
                return@use
            }
            val source = resp.body?.source() ?: run {
                parseEvent(AiSseEventDto(type = "error", message = "AI 服务暂时不可用"))?.let { emit(it) }
                return@use
            }
            // 读取 SSE 流事件
            while (!ended) {
                val line = runInterruptible(Dispatchers.IO) { source.readUtf8Line() } ?: break
                if (line.startsWith("{")) {
                    val message = parseErrorMessage(line)
                    parseEvent(AiSseEventDto(type = "error", message = message))?.let { emit(it) }
                    ended = true
                    continue
                }
                // 业务错误
                if (!line.startsWith("data:")) continue
                val payload = line.removePrefix("data:").trim()
                if (payload.isEmpty() || payload == "[DONE]") continue
                val dto = runCatching { gson.fromJson(payload, AiSseEventDto::class.java) }.getOrNull()
                    ?: continue
                parseEvent(dto)?.let { emit(it) }
                if (dto.type == "done" || dto.type == "error") ended = true
            }
            if (!ended) {
                parseEvent(AiSseEventDto(type = "error", message = "AI 服务暂时不可用"))?.let { emit(it) }
            }
        }
    } catch (ce: CancellationException) {
        throw ce
    } catch (e: IOException) {
        if (!ended) {
            parseEvent(AiSseEventDto(type = "error", message = NETWORK_ERROR_MESSAGE))?.let { emit(it) }
        }
    }
}

/// 解析 http 错误响应体
private fun parseErrorMessage(body: String?): String {
    if (body.isNullOrBlank()) return "AI 服务暂时不可用"
    return runCatching {
        val type = object : TypeToken<ApiResponse<Any?>>() {}.type
        val parsed: ApiResponse<Any?> = Gson().fromJson(body, type)
        parsed.message.ifBlank { "AI 服务暂时不可用" }
    }.getOrDefault("AI 服务暂时不可用")
}
