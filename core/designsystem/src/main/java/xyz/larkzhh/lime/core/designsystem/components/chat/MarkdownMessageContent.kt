package xyz.larkzhh.lime.core.designsystem.components.chat

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mikepenz.markdown.coil3.Coil3ImageTransformerImpl
import com.mikepenz.markdown.compose.components.markdownComponents
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.elements.MarkdownCheckBox
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
        h1 = headingStyle(20),
        h2 = headingStyle(18),
        h3 = headingStyle(17),
        h4 = headingStyle(16),
        h5 = headingStyle(15),
        h6 = headingStyle(14),
    )
    val components = markdownComponents(
        checkbox = { MarkdownCheckBox(it.content, it.node, it.typography.text) },
        horizontalRule = { Spacer(modifier = Modifier.height(10.dp)) },
    )
    Markdown(
        content = content,
        colors = markdownColor(),
        typography = typography,
        components = components,
        modifier = modifier,
        imageTransformer = Coil3ImageTransformerImpl,
    )
}

/// 标题样式
@Composable
private fun headingStyle(sizeSp: Int): TextStyle {
    val base = MaterialTheme.typography.titleMedium
    return base.copy(
        fontSize = sizeSp.sp,
        lineHeight = (sizeSp * 1.35f).sp,
        fontWeight = FontWeight.Bold,
    )
}
