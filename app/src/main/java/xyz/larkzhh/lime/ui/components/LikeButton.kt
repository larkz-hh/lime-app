package xyz.larkzhh.lime.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import xyz.larkzhh.lime.R
import xyz.larkzhh.lime.ui.theme.LimeGray

/**
 * 点赞按钮
 *
 * @param liked 是否已点赞
 * @param onToggle 点击回调
 * @param lottieAsset assets 目录下的动画文件路径，默认 "lottie/like.lottie"
 * @param iconSize 静态爱心图标大小
 * @param animationSize Lottie 动画大小
 * @param modifier 外部传入的 Modifier
 */
@Composable
fun LikeButton(
    liked: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    lottieAsset: String = "lottie/like.lottie",
    iconSize: Dp = 14.dp,
    animationSize: Dp = 32.dp,
    inactiveColor: Color = LimeGray,
) {
    val composition by rememberLottieComposition(LottieCompositionSpec.Asset(lottieAsset))
    var isAnimating by remember { mutableStateOf(false) }
    val progress by animateLottieCompositionAsState(
        composition = composition,
        isPlaying = isAnimating,
        iterations = 1,
        restartOnPlay = true,
    )
    LaunchedEffect(progress) {
        if (progress >= 1f && isAnimating) isAnimating = false
    }

    Box(
        modifier = modifier
            .size(animationSize)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
            ) {
                if (!liked) isAnimating = true
                onToggle()
            },
        contentAlignment = Alignment.Center,
    ) {
        if (liked && isAnimating) {
            LottieAnimation(
                composition = composition,
                progress = { progress },
                modifier = Modifier.size(animationSize),
            )
        } else {
            if (liked) {
                Icon(
                    imageVector = Icons.Filled.Favorite,
                    contentDescription = "取消点赞",
                    modifier = Modifier.size(iconSize),
                    tint = Color.Red,
                )
            } else {
                Icon(
                    painter = painterResource(R.drawable.ic_heart_outline),
                    contentDescription = "点赞",
                    modifier = Modifier.size(iconSize),
                    tint = inactiveColor,
                )
            }
        }
    }
}
