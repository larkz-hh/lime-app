package xyz.larkzhh.lime.ui.publish.components
import androidx.compose.material3.MaterialTheme

import android.content.Context
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.video.videoFrameMillis
import kotlin.math.roundToLong

/// 滑轨缩略图数量
private const val THUMB_COUNT = 8

/// 截帧滑轨
@Composable
fun FrameScrubTrack(
    videoUri: Uri,
    durationMs: Long,
    currentMs: Long,
    selected: Boolean,
    onScrub: (Long) -> Unit,
    context: Context,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    var handleX by remember { mutableLongStateOf(currentMs) }
    LaunchedEffect(currentMs) { handleX = currentMs }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(6.dp)),
    ) {
        val trackWidthPx = with(density) { maxWidth.toPx() }

        // 缩略图轨道
        Row(modifier = Modifier.fillMaxSize()) {
            repeat(THUMB_COUNT) { i ->
                val t = durationMs * i / THUMB_COUNT
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(videoUri)
                        .videoFrameMillis(t)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize(),
                )
            }
        }

        // 未生效时压暗轨道
        if (!selected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.35f))
            )
        }

        // 拖动手柄
        val handleFraction = if (durationMs > 0) handleX.toFloat() / durationMs else 0f
        val handleWidthDp = 4.dp
        val handleOffsetDp = with(density) {
            ((trackWidthPx - handleWidthDp.toPx()) * handleFraction).toDp()
        }
        Box(
            modifier = Modifier
                .padding(start = handleOffsetDp)
                .width(handleWidthDp)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
        )

        //轨道的拖动、点按手势
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(durationMs, trackWidthPx) {
                    val update = { x: Float ->
                        val fraction = (x / trackWidthPx).coerceIn(0f, 1f)
                        val newMs = (fraction * durationMs).roundToLong()
                        handleX = newMs
                        onScrub(newMs)
                    }
                    detectDragGestures(
                        onDragStart = { offset -> update(offset.x) },
                        onDrag = { change, _ ->
                            change.consume()
                            update(change.position.x)
                        },
                    )
                }
        )
    }
}
