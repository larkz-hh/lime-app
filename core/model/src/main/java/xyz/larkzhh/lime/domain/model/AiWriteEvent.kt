package xyz.larkzhh.lime.domain.model

/**
 * AI 写作辅助流式事件
 */
sealed interface AiWriteEvent {
    data class Delta(val content: String) : AiWriteEvent// 增量文本
    data class Done(val content: String, val model: String?) : AiWriteEvent// 生成结束
    data class Error(val message: String) : AiWriteEvent// 生成失败
}
