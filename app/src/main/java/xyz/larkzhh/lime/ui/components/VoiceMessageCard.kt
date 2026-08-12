package xyz.larkzhh.lime.ui.components

import android.media.MediaPlayer
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import xyz.larkzhh.lime.R
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import xyz.larkzhh.lime.ui.theme.LimeDark
import xyz.larkzhh.lime.ui.theme.LimeLightGray
import java.io.File

/**
 * 语音消息卡片（用于评论列表展示或入框预览）。
 *
 * @param url 网络语音 URL 或 localFile
 * @param localFile 本地录音文件或 url
 * @param durationSeconds 语音时长（秒）
 * @param isCurrentlyPlaying 是否由外部控制正在播放，多卡片互斥
 * @param onPlayStart 开始播放时回调，外部互斥
 * @param onPlayStop 停止播放时回调
 */
@Composable
fun VoiceMessageCard(
    durationSeconds: Int,
    modifier: Modifier = Modifier,
    url: String? = null,
    localFile: File? = null,
    isCurrentlyPlaying: Boolean = false,
    onPlayStart: () -> Unit = {},
    onPlayStop: () -> Unit = {},
    backgroundColor: Color = LimeLightGray,
    textColor: Color = LimeDark,
) {
    var isPlaying by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    LaunchedEffect(isCurrentlyPlaying) {
        if (!isCurrentlyPlaying && isPlaying) {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
            isPlaying = false
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
        }
    }

    fun togglePlay() {
        if (isLoading) return
        if (isPlaying) {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
            isPlaying = false
            onPlayStop()
            return
        }
        val source = url ?: localFile?.absolutePath ?: return//  获取播放源
        val player = MediaPlayer()
        try {
            if (url != null) {
                isLoading = true
                player.setDataSource(source)
                player.prepareAsync()
                player.setOnPreparedListener {
                    isLoading = false
                    it.start()
                    isPlaying = true
                    onPlayStart()
                }
            } else {
                player.setDataSource(source)
                player.prepare()
                player.start()
                isPlaying = true
                onPlayStart()
            }
            player.setOnCompletionListener {
                it.release()
                mediaPlayer = null
                isPlaying = false
                onPlayStop()
            }
            player.setOnErrorListener { _, _, _ ->
                player.release()
                mediaPlayer = null
                isPlaying = false
                isLoading = false
                onPlayStop()
                true
            }
            mediaPlayer = player
        } catch (_: Exception) {
            player.release()
            isLoading = false
        }
    }

    val lottieComposition by rememberLottieComposition(LottieCompositionSpec.Asset("lottie/voice.lottie"))
    val lottieProgress by animateLottieCompositionAsState(
        composition = lottieComposition,
        isPlaying = isPlaying,
        iterations = LottieConstants.IterateForever,
    )

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .clickable { togglePlay() }
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(modifier = Modifier.size(22.dp), contentAlignment = Alignment.Center) {
            if (isPlaying && lottieComposition != null) {
                LottieAnimation(
                    composition = lottieComposition,
                    progress = { lottieProgress },
                    modifier = Modifier.size(22.dp),
                )
            } else {
                Icon(
                    painter = painterResource(id = R.drawable.ic_voice),
                    contentDescription = null,
                    tint = LimeDark,
                    modifier = Modifier.size(20.dp),
                )
            }
        }

        Spacer(modifier = Modifier.width(2.dp))

        Text(
            text = "${durationSeconds}\"",
            fontSize = 14.sp,
            color = textColor,
        )
    }
}
