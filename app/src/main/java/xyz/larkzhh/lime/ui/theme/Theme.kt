package xyz.larkzhh.lime.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily

@Composable
fun LimeTheme(
    // 深浅色偏好
    darkTheme: Boolean = AppDarkMode.effectiveDark(isSystemInDarkTheme()),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    // 当前主题
    val themeColors = themeColorMap[ThemeOption.fromTag(AppTheme.currentTag())]
        ?: themeColorMap.getValue(ThemeOption.GREEN)
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> darkColorScheme(
            primary = themeColors.darkPrimary,
            secondary = LimeGray,
            tertiary = Color(0xFF3E7A5E),
            secondaryContainer = Color(0xFF24402F),
            background = LimeDark,
            surface = LimeSurface,
            surfaceVariant = Color(0xFF1B1B1B),
            surfaceContainer = Color(0xFF2E2E2E),
            surfaceContainerLowest = Color.Black,
            onPrimary = Color.White,
            onBackground = Color.White,
            onSurface = Color.White,
            onSurfaceVariant = Color(0xFFB0B0B0),
        )

        else -> lightColorScheme(
            primary = themeColors.lightPrimary,
            secondary = LimeGray,
            tertiary = LimePrimaryPale,
            secondaryContainer = LimePrimaryPale,
            background = LimeLightGray,
            surface = Color.White,
            surfaceVariant = Color(0xFFF1F1F1),
            surfaceContainer = Color.White,
            surfaceContainerLowest = Color.White,
            onPrimary = Color.White,
            onBackground = LimeDark,
            onSurface = LimeDark,
            onSurfaceVariant = LimeGray,
        )
    }

    // 当前字体
    val fontOption = FontOption.fromTag(AppFont.currentTag())
    val fontFamily = fontFamilyOf(fontOption, LocalContext.current) ?: FontFamily.Default

    CompositionLocalProvider(
        LocalChatBubbleColors provides if (darkTheme) DarkChatBubbleColors else LightChatBubbleColors,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography.withFontFamily(fontFamily),
            content = content,
        )
    }
}
