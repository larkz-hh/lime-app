package xyz.larkzhh.lime.ui.components.chat

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun TypingIndicator(
    modifier: Modifier = Modifier,
    dotColor: Color = Color(0xFF8A8A8A),
) {
    val transition = rememberInfiniteTransition(label = "typing")
    val dotCount = 3
    val phases = List(dotCount) { index ->
        val offset by transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 360, delayMillis = index * 150, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "dot$index",
        )
        offset
    }
    Row(modifier = modifier) {
        phases.forEach { phase ->
            Box(
                modifier = Modifier
                    .padding(horizontal = 2.dp)
                    .size(7.dp)
                    .alpha(0.35f + 0.65f * phase)
                    .offset(y = (-3f * phase).dp)
                    .clip(CircleShape)
                    .background(dotColor),
            )
        }
    }
}
