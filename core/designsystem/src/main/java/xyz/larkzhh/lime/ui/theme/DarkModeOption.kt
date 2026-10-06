package xyz.larkzhh.lime.ui.theme

/// 深色模式选项
enum class DarkModeOption(val tag: String) {
    SYSTEM("system"),
    DARK("dark"),
    ;

    companion object {
        fun fromTag(tag: String?): DarkModeOption =
            entries.firstOrNull { it.tag == tag } ?: SYSTEM
    }
}

/// 是否深色
fun DarkModeOption.isDark(systemDark: Boolean): Boolean = when (this) {
    DarkModeOption.SYSTEM -> systemDark
    DarkModeOption.DARK -> true
}
