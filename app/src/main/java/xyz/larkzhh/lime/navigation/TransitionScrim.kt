package xyz.larkzhh.lime.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/// 返回手势压暗状态
object SwipeBackScrimState {
    var revealEntryId by mutableStateOf<String?>(null)
    var progress by mutableFloatStateOf(0f)
}

/// 转场暗色遮罩容器
@Composable
fun ScrimBox(
    entryId: String?,
    maxAlpha: Float = 0.3f,
    content: @Composable () -> Unit,
) {
    Box(Modifier.fillMaxSize()) {
        content()
        val isRevealTarget = entryId != null && SwipeBackScrimState.revealEntryId == entryId
        val alpha = if (isRevealTarget) {
            ((1f - SwipeBackScrimState.progress) * maxAlpha).coerceIn(0f, maxAlpha)
        } else 0f
        if (alpha > 0.001f) {
            Box(Modifier.matchParentSize().background(Color.Black.copy(alpha = alpha)))
        }
    }
}
