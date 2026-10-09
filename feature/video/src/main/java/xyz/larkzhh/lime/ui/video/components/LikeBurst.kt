package xyz.larkzhh.lime.ui.video.components

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition

@Composable
fun BoxScope.LikeBurst(
    position: Offset?,
    triggerKey: Int,
    onFinished: () -> Unit = {},
) {
    if (position == null || triggerKey == 0) return
    val density = LocalDensity.current
    val sizeDp = 160.dp
    val half = with(density) { (sizeDp / 2).toPx() }
    // 双击重建
    key(triggerKey) {
        val composition by rememberLottieComposition(
            LottieCompositionSpec.Asset("lottie/like.lottie"),
        )
        val progress by animateLottieCompositionAsState(
            composition = composition,
            isPlaying = true,
            iterations = 1,
            restartOnPlay = true,
        )
        if (progress < 1f) {
            LottieAnimation(
                composition = composition,
                progress = { progress },
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset {
                        IntOffset(
                            (position.x - half).toInt(),
                            (position.y - half).toInt(),
                        )// 动画中心移动到点击中心
                    }
                    .size(sizeDp),
            )
        } else {
            LaunchedEffect(Unit) { onFinished() }// 清除状态
        }
    }
}
