package xyz.larkzhh.lime.ui.components

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import xyz.larkzhh.lime.domain.model.FollowActionState
import xyz.larkzhh.lime.domain.model.label
import xyz.larkzhh.lime.ui.theme.LimeGray
import xyz.larkzhh.lime.ui.theme.LimePrimary

/**
 * 通用关注按钮
 */
@Composable
fun FollowButton(
    state: FollowActionState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val followed = state != FollowActionState.Follow
    val borderColor = if (followed) LimeGray else LimePrimary
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .border(BorderStroke(1.dp, borderColor), RoundedCornerShape(50))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClick,
            )
            .padding(horizontal = 16.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = state.label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = borderColor,
        )
    }
}
