package xyz.larkzhh.lime.ui.group

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import xyz.larkzhh.lime.ui.theme.LimeRed

/// 卡片式输框
@Composable
fun GroupFieldCard(
    label: String,
    required: Boolean,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    maxLength: Int,
    singleLine: Boolean,
    minLines: Int = 1,
    enabled: Boolean = true,
) {
    val textColor = if (enabled) MaterialTheme.colorScheme.onSurface
    else MaterialTheme.colorScheme.onSurfaceVariant
    val cardShape = RoundedCornerShape(12.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = if (enabled) 2.dp else 0.dp,
                shape = cardShape,
                clip = false,
                ambientColor = Color(0x33000000),
                spotColor = Color(0x33000000),
            )
            .clip(cardShape)
            .background(
                if (enabled) MaterialTheme.colorScheme.surface
                else MaterialTheme.colorScheme.surfaceVariant
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textColor)
            if (required) {
                Spacer(Modifier.width(3.dp))
                Text("*", color = LimeRed, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(12.dp))
        BasicTextField(
            value = value,
            onValueChange = { onValueChange(it.take(maxLength)) },
            enabled = enabled,
            singleLine = singleLine,
            minLines = if (singleLine) 1 else minLines,
            textStyle = LocalTextStyle.current.copy(fontSize = 16.sp, color = textColor),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = if (singleLine) 22.dp else (minLines * 24).dp),
            decorationBox = { innerTextField ->
                Box {
                    if (value.isEmpty()) {
                        Text(
                            placeholder,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (enabled) 1f else 0.6f),
                        )
                    }
                    innerTextField()
                }
            },
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "${value.length}/$maxLength",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.End,
        )
    }
}
