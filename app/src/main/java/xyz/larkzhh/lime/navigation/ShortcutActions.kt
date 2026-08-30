package xyz.larkzhh.lime.navigation

/**
 * APP 快捷入口
 */
object ShortcutActions {

    const val SEARCH = "xyz.larkzhh.lime.action.SEARCH"
    const val AI_CHAT = "xyz.larkzhh.lime.action.AI_CHAT"
    const val QR_SCAN = "xyz.larkzhh.lime.action.QR_SCAN"

    // 是否为快捷入口
    fun isShortcut(action: String?): Boolean =
        action == SEARCH || action == AI_CHAT || action == QR_SCAN
}
