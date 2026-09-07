package xyz.larkzhh.lime.navigation.state

import xyz.larkzhh.lime.domain.model.ChatNote

object PendingChatStore {

    // AI 引用的笔记
    var askAiNote: ChatNote? = null

    // AI 输入框的文本
    var askAiText: String? = null
}
