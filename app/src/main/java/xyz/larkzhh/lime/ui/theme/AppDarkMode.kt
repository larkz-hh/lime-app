package xyz.larkzhh.lime.ui.theme

import com.tencent.mmkv.MMKV

/**
 * 深色模式
 */
object AppDarkMode {

    private const val KEY_NAME = "dark_mode"

    private val mmkv: MMKV by lazy { MMKV.defaultMMKV() }

    fun currentTag(): String =
        mmkv.decodeString(KEY_NAME, DarkModeOption.SYSTEM.tag) ?: DarkModeOption.SYSTEM.tag

    fun setTag(tag: String) {
        mmkv.encode(KEY_NAME, tag)
    }

    /// 是否深色
    fun effectiveDark(systemDark: Boolean): Boolean = when (DarkModeOption.fromTag(currentTag())) {
        DarkModeOption.SYSTEM -> systemDark
        DarkModeOption.DARK -> true
    }
}

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
