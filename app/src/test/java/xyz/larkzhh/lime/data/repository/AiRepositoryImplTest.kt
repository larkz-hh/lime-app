package xyz.larkzhh.lime.data.repository

import com.google.gson.Gson
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import xyz.larkzhh.lime.data.network.ai.AiApi
import xyz.larkzhh.lime.data.network.model.AiChatCancelRequest
import xyz.larkzhh.lime.data.network.model.AiModelDto
import xyz.larkzhh.lime.data.network.model.AiTranslateRequest
import xyz.larkzhh.lime.data.network.model.AiTranslateResponse
import xyz.larkzhh.lime.data.network.model.ApiResponse
import xyz.larkzhh.lime.data.network.model.ChatMessageDto
import xyz.larkzhh.lime.data.network.model.ConversationListData

/// AI 仓库
class AiRepositoryImplTest {

    private val api = FakeAiApi()
    private val repository = AiRepositoryImpl(
        apiService = api,
        sseClient = OkHttpClient(),
        baseUrl = "http://localhost/",
        gson = Gson(),
    )

    @Test
    fun 翻译成功返回译文() = runBlocking {
        api.translateResponse = ApiResponse(200, "ok", AiTranslateResponse("Hello"))

        assertEquals("Hello", repository.translate("你好", "en", null).getOrThrow())
    }

    @Test
    fun 译文中的data标签被剥离() = runBlocking {
        api.translateResponse = ApiResponse(200, "ok", AiTranslateResponse("<data>Hello</data>"))

        assertEquals("Hello", repository.translate("你好", "en", null).getOrThrow())
    }

    @Test
    fun 多个data标签全部剥离并去掉首尾空白() = runBlocking {
        api.translateResponse = ApiResponse(200, "ok", AiTranslateResponse("  <data>a</data><data>b</data>  "))

        assertEquals("ab", repository.translate("x", "en", null).getOrThrow())
    }

    @Test
    fun 透传翻译参数() = runBlocking {
        api.translateResponse = ApiResponse(200, "ok", AiTranslateResponse("x"))

        repository.translate("你好", "en", "zh")

        assertEquals("你好", api.lastRequest?.text)
        assertEquals("en", api.lastRequest?.targetLang)
        assertEquals("zh", api.lastRequest?.sourceLang)
    }

    @Test
    fun 翻译非200时失败并带上服务端消息() = runBlocking {
        api.translateResponse = ApiResponse(500, "模型繁忙", null)

        val result = repository.translate("你好", "en", null)

        assertTrue(result.isFailure)
        assertEquals("模型繁忙", result.exceptionOrNull()?.message)
    }

    @Test
    fun 翻译code为200但data为空也算失败() = runBlocking {
        api.translateResponse = ApiResponse(200, "ok", null)

        assertTrue(repository.translate("你好", "en", null).isFailure)
    }

    private class FakeAiApi : AiApi {
        var translateResponse: ApiResponse<AiTranslateResponse> = ApiResponse(200, "ok", null)
        var lastRequest: AiTranslateRequest? = null

        override suspend fun aiTranslate(request: AiTranslateRequest): ApiResponse<AiTranslateResponse> {
            lastRequest = request
            return translateResponse
        }

        override suspend fun getAiModels(): ApiResponse<List<AiModelDto>> = error("未使用")

        override suspend fun cancelAiGeneration(request: AiChatCancelRequest): ApiResponse<Unit> = error("未使用")

        override suspend fun getConversations(cursor: String?, size: Int): ApiResponse<ConversationListData> =
            error("未使用")

        override suspend fun getConversationMessages(conversationId: String): ApiResponse<List<ChatMessageDto>> =
            error("未使用")

        override suspend fun deleteConversation(conversationId: String): ApiResponse<Unit> = error("未使用")

        override suspend fun deleteConversationMessage(
            conversationId: String,
            messageId: Long,
        ): ApiResponse<Unit> = error("未使用")

        override suspend fun clearConversationMessages(conversationId: String): ApiResponse<Unit> = error("未使用")
    }
}
