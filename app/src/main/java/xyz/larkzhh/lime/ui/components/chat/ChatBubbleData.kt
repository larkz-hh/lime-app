package xyz.larkzhh.lime.ui.components.chat

/**
 * 通用聊天气泡数据模型
 */
data class ChatBubbleData(
    val id: Long,
    val isSelf: Boolean,
    val content: String,
    val images: List<String> = emptyList(),
    val avatarUrl: String? = null,
    val nickname: String? = null,
    val timeText: String? = null,
    val timestamp: Long? = null,// epoch 毫秒
    val status: ChatBubbleStatus = ChatBubbleStatus.DONE,
    val renderMarkdown: Boolean = false,// 内容是否为 Markdown
)

enum class ChatBubbleStatus {
    SENDING,   // 发送中
    STREAMING, // 流式输出中
    DONE,      // 完成
    FAILED,    // 失败
    STOPPED,   // 已停止
}

/// 输入栏待发送图片
data class ChatInputImage(
    val uri: String,
    val state: ChatInputImageState = ChatInputImageState.PENDING,
)

enum class ChatInputImageState {
    PENDING,   // 待上传
    UPLOADING, // 上传中
    FAILED,    // 上传失败
}
