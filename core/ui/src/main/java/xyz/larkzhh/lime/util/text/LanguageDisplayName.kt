package xyz.larkzhh.lime.util.text

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import xyz.larkzhh.lime.core.designsystem.R as DesignSystemR

@Composable
fun languageDisplayName(tag: String): String = when (tag) {
    AppLanguage.TAG_SIMPLIFIED -> "简体中文"
    AppLanguage.TAG_TRADITIONAL -> "繁體中文"
    AppLanguage.TAG_ENGLISH -> "English"
    else -> stringResource(DesignSystemR.string.lang_system)
}
