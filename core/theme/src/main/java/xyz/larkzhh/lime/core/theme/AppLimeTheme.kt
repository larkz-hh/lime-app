package xyz.larkzhh.lime.core.theme

import androidx.compose.foundation.isSystemInDarkTheme
import xyz.larkzhh.lime.ui.theme.DarkModeOption
import xyz.larkzhh.lime.ui.theme.FontOption
import xyz.larkzhh.lime.ui.theme.LimeTheme
import xyz.larkzhh.lime.ui.theme.ThemeOption
import androidx.compose.runtime.Composable

/// 读取本地偏好后套用设计系统主题
@Composable
fun AppLimeTheme(
    darkTheme: Boolean = AppDarkMode.effectiveDark(isSystemInDarkTheme()),
    content: @Composable () -> Unit,
) = LimeTheme(
    themeOption = ThemeOption.fromTag(AppTheme.currentTag()),
    fontOption = FontOption.fromTag(AppFont.currentTag()),
    darkTheme = darkTheme,
    content = content,
)
