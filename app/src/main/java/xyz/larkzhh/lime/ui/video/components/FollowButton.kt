package xyz.larkzhh.lime.ui.video.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import xyz.larkzhh.lime.ui.theme.LimePrimary
import xyz.larkzhh.lime.ui.theme.LimeWhite

/// 关注按钮
@Composable
fun FollowButton(
    followed: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    outlineColor: Color = LimeWhite,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .then(
                if (followed) Modifier.border(
                    BorderStroke(1.dp, outlineColor.copy(alpha = 0.6f)),
                    RoundedCornerShape(50),
                ) else Modifier.background(LimePrimary)
            )
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClick,
            )
            .padding(horizontal = 16.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = if (followed) "已关注" else "关注",
            color = if (followed) outlineColor else LimeWhite,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}
