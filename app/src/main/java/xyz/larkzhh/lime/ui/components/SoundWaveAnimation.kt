package xyz.larkzhh.lime.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.LottieProperty
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.airbnb.lottie.compose.rememberLottieDynamicProperties
import com.airbnb.lottie.compose.rememberLottieDynamicProperty
import xyz.larkzhh.lime.ui.theme.LimeGray

/// 声波动画资源
private const val WAVE_ASSET = "lottie/soundwave.lottie"

private const val WAVE_WIDTH_DP = 160
private const val WAVE_HEIGHT_DP = 64

/**
 * 录音声波动画
 *
 * 振幅驱动播放速度
 *
 * @param isRecording 是否正在录音
 * @param amplitude 归一化振幅（0f~1f）
 */
@Composable
internal fun SoundWaveAnimation(
    isRecording: Boolean,
    amplitude: Float,
    modifier: Modifier = Modifier,
) {
    val composition by rememberLottieComposition(LottieCompositionSpec.Asset(WAVE_ASSET))
    // 声音驱动播放速度
    val speed = if (isRecording) 0.5f + amplitude * 0.7f else 1f
    val progress by animateLottieCompositionAsState(
        composition = composition,
        isPlaying = isRecording,
        iterations = LottieConstants.IterateForever,
        speed = speed,
    )
    val waveColor = if (isRecording) Color.Black else LimeGray
    val dynamicProperties = rememberLottieDynamicProperties(
        rememberLottieDynamicProperty(LottieProperty.COLOR, waveColor.toArgb(), "**"),
        rememberLottieDynamicProperty(LottieProperty.STROKE_COLOR, waveColor.toArgb(), "**"),
    )
    LottieAnimation(
        composition = composition,
        progress = { if (isRecording) progress else 0f },
        dynamicProperties = dynamicProperties,
        modifier = modifier.size(WAVE_WIDTH_DP.dp, WAVE_HEIGHT_DP.dp),
    )
}
