package xyz.larkzhh.lime.ui.components.video

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import xyz.larkzhh.lime.domain.model.FollowActionState
import xyz.larkzhh.lime.ui.components.LoginGate
import xyz.larkzhh.lime.ui.components.labelRes

/// 关注按钮
@Composable
fun FollowButton(
    state: FollowActionState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    outlineColor: Color? = null,
) {
    val followed = state != FollowActionState.Follow
    val outline = outlineColor ?: MaterialTheme.colorScheme.outline
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .then(
                if (followed) Modifier.border(
                    BorderStroke(1.dp, outline.copy(alpha = 0.6f)),
                    RoundedCornerShape(50),
                ) else Modifier.background(MaterialTheme.colorScheme.primary)
            )
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = {
                    // 关注登录拦截
                    if (!LoginGate.onRequireLogin(null)) onClick()
                },
            )
            .padding(horizontal = 16.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(state.labelRes),
            color = if (followed) outline else MaterialTheme.colorScheme.onPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}
