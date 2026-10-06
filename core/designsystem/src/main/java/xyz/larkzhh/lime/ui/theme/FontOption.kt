package xyz.larkzhh.lime.ui.theme

import android.content.Context
import android.graphics.Typeface
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import xyz.larkzhh.lime.core.designsystem.R

/// 可选字体
enum class FontOption(
    val tag: String,
    val displayName: String,
    val assetPath: String?,
    @StringRes val labelRes: Int?,
) {
    SYSTEM("system", "系统默认", null, R.string.font_option_system),
    ZHUNYUAN("zhunyuan", "方正准圆", "fonts/fz_zhunyuan.ttf", null),
    WAWA("wawa", "华康娃娃体", "fonts/hk_wawa.ttf", null),
    KAITI("kaiti", "楷体", "fonts/kaiti.ttf", null),
    ;

    companion object {
        fun fromTag(tag: String?): FontOption =
            entries.firstOrNull { it.tag == tag } ?: SYSTEM
    }
}

/// 选项显示名
@Composable
fun fontDisplayName(option: FontOption): String =
    option.labelRes?.let { stringResource(it) } ?: option.displayName

/// 已加载字体缓存
private val fontCache = mutableMapOf<String, FontFamily>()

/// 选项对应 FontFamily
fun fontFamilyOf(option: FontOption, context: Context): FontFamily? {
    val path = option.assetPath ?: return null
    return fontCache.getOrPut(path) {
        FontFamily(Typeface.createFromAsset(context.assets, path))
    }
}
