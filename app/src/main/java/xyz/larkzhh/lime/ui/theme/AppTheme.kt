package xyz.larkzhh.lime.ui.theme

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import com.tencent.mmkv.MMKV
import xyz.larkzhh.lime.R
import xyz.larkzhh.lime.data.local.TokenStorage
import xyz.larkzhh.lime.data.local.UserPreferences

/**
 * 应用主题
 */
object AppTheme {

    private const val KEY_NAME = UserPreferences.Keys.THEME

    val defaultTag: String = ThemeOption.GREEN.tag

    private val mmkv: MMKV by lazy { MMKV.defaultMMKV() }

    private fun storageKey(): String {
        val uid = mmkv.decodeLong(TokenStorage.KEY_CURRENT_USER_ID, -1L)
        return if (uid > 0) "pref_${uid}_$KEY_NAME" else "pref_$KEY_NAME"
    }

    fun currentTag(): String = mmkv.decodeString(storageKey(), defaultTag) ?: defaultTag

    fun setTag(tag: String) {
        mmkv.encode(storageKey(), tag)
    }
}

/// 可选主题
enum class ThemeOption(val tag: String, @StringRes val labelRes: Int) {
    GREEN("green", R.string.theme_green),
    RED("red", R.string.theme_red),
    ORANGE("orange", R.string.theme_orange),
    BLUE("blue", R.string.theme_blue),
    PURPLE("purple", R.string.theme_purple),
    ;

    companion object {
        fun fromTag(tag: String?): ThemeOption =
            entries.firstOrNull { it.tag == tag } ?: GREEN
    }
}

/// 单套主题主色
data class ThemeColors(
    val lightPrimary: Color,
    val darkPrimary: Color,
    val ballPrimary: Color,// 底部栏小球色
)

/// 主题主色
val themeColorMap: Map<ThemeOption, ThemeColors> = mapOf(
    // 青柠绿
    ThemeOption.GREEN to ThemeColors(
        lightPrimary = Color(0xFF4A9B6F),
        darkPrimary = Color(0xFF6DB890),
        ballPrimary = Color(0xFFA8E743),
    ),
    // 小红书红
    ThemeOption.RED to ThemeColors(
        lightPrimary = Color(0xFFFE2C55),
        darkPrimary = Color(0xFFFE2C55),
        ballPrimary = Color(0xFFFF8FA3),
    ),
    // 活力橙
    ThemeOption.ORANGE to ThemeColors(
        lightPrimary = Color(0xFFFF7A00),
        darkPrimary = Color(0xFFFFA040),
        ballPrimary = Color(0xFFFFB25E),
    ),
    // 克莱因蓝
    ThemeOption.BLUE to ThemeColors(
        lightPrimary = Color(0xFF2E5BFF),
        darkPrimary = Color(0xFF6E9BFF),
        ballPrimary = Color(0xFF8FB3FF),
    ),
    // 紫
    ThemeOption.PURPLE to ThemeColors(
        lightPrimary = Color(0xFF7C4DFF),
        darkPrimary = Color(0xFFB39DDB),
        ballPrimary = Color(0xFFCBA6FF),
    ),
)
