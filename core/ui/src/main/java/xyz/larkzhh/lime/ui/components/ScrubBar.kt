package xyz.larkzhh.lime.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Slider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/// 进度条
@kotlin.OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScrubBar(
    fraction: Float,
    onDragStart: () -> Unit,
    onSeek: (Float) -> Unit,
    onDragEnd: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    var dragValue by remember { mutableFloatStateOf(fraction) }
    var dragging by remember { mutableStateOf(false) }
    // 未拖动时跟随播放进度
    LaunchedEffect(fraction) { if (!dragging) dragValue = fraction }

    Slider(
        value = dragValue,
        onValueChange = { v ->
            if (!dragging) {
                dragging = true
                onDragStart()
            }
            dragValue = v
            onSeek(v)
        },
        onValueChangeFinished = {
            onDragEnd(dragValue)
            dragging = false
        },
        modifier = modifier,
        thumb = {
            Box(
                modifier = Modifier.height(16.dp),
                contentAlignment = Alignment.BottomCenter,
            ) {
                Box(
                    modifier = Modifier
                        // 14.5-(16-6)
                        .offset(y = 4.5.dp)
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface),
                )
            }
        },
        track = { state ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(16.dp),
                contentAlignment = Alignment.BottomStart,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color.White.copy(alpha = 0.3f)),
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth(state.value.coerceIn(0f, 1f))
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(MaterialTheme.colorScheme.surface),
                )
            }
        },
    )
}

/// 毫秒转 mm:ss
fun formatTime(ms: Long): String {
    val totalSec = ms / 1000
    val m = totalSec / 60
    val s = totalSec % 60
    return "%02d:%02d".format(m, s)
}
