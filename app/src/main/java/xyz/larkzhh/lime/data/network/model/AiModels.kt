package xyz.larkzhh.lime.data.network.model

/// AI 翻译请求
data class AiTranslateRequest(
    val text: String,
    val targetLang: String,
    val sourceLang: String? = null,
    val model: String? = null,
)

/// AI 翻译响应
data class AiTranslateResponse(
    val translatedText: String,
    val sourceLang: String? = null,
    val targetLang: String? = null,
)
