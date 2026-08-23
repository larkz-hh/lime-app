package xyz.larkzhh.lime.ui.video.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp

/// 可展开正文
@Composable
fun ExpandableText(
    title: String?,
    body: String?,
    modifier: Modifier = Modifier,
    textColor: Color = Color.White,
    collapsedMaxLines: Int = 2,
) {
    var expanded by remember(title, body) { mutableStateOf(false) }
    var hasOverflow by remember(title, body) { mutableStateOf(false) }

    // 加粗标题与正文
    val text = buildAnnotatedString {
        if (!title.isNullOrBlank()) {
            withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append(title) }
            if (!body.isNullOrBlank()) append(" ")
        }
        if (!body.isNullOrBlank()) append(body)
    }

    if (text.isEmpty()) return

    Column(modifier = modifier) {
        Text(
            text = text,
            color = textColor.copy(alpha = 0.92f),
            fontSize = 14.sp,
            maxLines = if (expanded) Int.MAX_VALUE else collapsedMaxLines,
            overflow = TextOverflow.Ellipsis,
            onTextLayout = { result ->
                if (!expanded) hasOverflow = result.hasVisualOverflow
            },
        )
        // 展开、收起
        if (expanded) {
            Text(
                text = "收起",
                color = textColor.copy(alpha = 0.6f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                ) { expanded = false },
            )
        } else if (hasOverflow) {
            Text(
                text = "展开",
                color = textColor.copy(alpha = 0.6f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                ) { expanded = true },
            )
        }
    }
}
