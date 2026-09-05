package xyz.larkzhh.lime.navigation

/**
 * APP 快捷入口
 */
object ShortcutActions {

    const val SEARCH = "xyz.larkzhh.lime.action.SEARCH"
    const val AI_CHAT = "xyz.larkzhh.lime.action.AI_CHAT"
    const val QR_SCAN = "xyz.larkzhh.lime.action.QR_SCAN"

    const val SEARCH_KEYWORD = "xyz.larkzhh.lime.action.SEARCH_KEYWORD"
    const val EXTRA_KEYWORD = "keyword"

    const val OPEN_MESSAGE = "xyz.larkzhh.lime.action.OPEN_MESSAGE"

    const val OPEN_IM_CHAT = "xyz.larkzhh.lime.action.OPEN_IM_CHAT"
    const val EXTRA_CONVERSATION_ID = "conversation_id"

    // 是否为快捷入口
    fun isShortcut(action: String?): Boolean =
        action == SEARCH || action == AI_CHAT || action == QR_SCAN ||
                action == SEARCH_KEYWORD || action == OPEN_MESSAGE || action == OPEN_IM_CHAT
}
