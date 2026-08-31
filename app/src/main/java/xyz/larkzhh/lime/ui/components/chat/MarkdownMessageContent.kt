package xyz.larkzhh.lime.ui.components.chat

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.mikepenz.markdown.coil3.Coil3ImageTransformerImpl
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.markdownColor
import com.mikepenz.markdown.m3.markdownTypography

/**
 * Markdown 消息内容渲染
 */
@Composable
fun MarkdownMessageContent(
    content: String,
    modifier: Modifier = Modifier,
) {
    val typography = markdownTypography(
        h1 = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.Bold),
        h2 = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold),
        h3 = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
        h4 = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
        h5 = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
        h6 = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
    )
    Markdown(
        content = content,
        colors = markdownColor(),
        typography = typography,
        modifier = modifier,
        imageTransformer = Coil3ImageTransformerImpl,
    )
}
