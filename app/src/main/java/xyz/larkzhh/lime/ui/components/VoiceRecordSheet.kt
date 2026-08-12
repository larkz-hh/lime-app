package xyz.larkzhh.lime.ui.components

import android.Manifest
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaRecorder
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.airbnb.lottie.compose.rememberLottieDynamicProperties
import com.airbnb.lottie.compose.rememberLottieDynamicProperty
import com.airbnb.lottie.LottieProperty
import androidx.compose.ui.graphics.toArgb
import kotlinx.coroutines.delay
import xyz.larkzhh.lime.ui.detail.comment.viewmodel.VoiceRecord
import xyz.larkzhh.lime.ui.theme.LimeGray
import xyz.larkzhh.lime.ui.theme.LimeLightGray
import xyz.larkzhh.lime.ui.theme.LimePrimary
import java.io.File
import kotlin.time.Duration.Companion.milliseconds

private const val MAX_DURATION_SECONDS = 60// 最大录音时长
private const val AMPLITUDE_POLL_MS = 80L// 振幅采样轮询时间间隔

/**
 * 录音浮层
 *
 * @param sheetTotalHeightDp 输入面板总高度
 * @param onVoiceRecorded 录音完成回调
 * @param onDismiss 关闭回调
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun VoiceRecordSheet(
    sheetTotalHeightDp: Int = 0,
    onVoiceRecorded: (VoiceRecord) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    var isRecording by remember { mutableStateOf(false) }
    var recordSeconds by remember { mutableIntStateOf(0) }// 录音已持续的秒数
    var isCancelling by remember { mutableStateOf(false) } // 上滑取消提示
    var currentAmplitude by remember { mutableFloatStateOf(0f) }

    var mediaRecorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var outputFile by remember { mutableStateOf<File?>(null) }
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    var audioFocusRequest by remember { mutableStateOf<AudioFocusRequest?>(null) }
    var focusLost by remember { mutableStateOf(false) }// 焦点被抢占时触发停录

    // 请求音频焦点
    fun requestAudioFocus() {
        val listener = AudioManager.OnAudioFocusChangeListener { change ->
            if (change == AudioManager.AUDIOFOCUS_LOSS || change == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
                focusLost = true// 被打断停止录音
            }
        }
        // 构建焦点请求配置
        val req = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            .setAcceptsDelayedFocusGain(false)
            .setOnAudioFocusChangeListener(listener)
            .build()
        audioFocusRequest = req
        audioManager.requestAudioFocus(req)
    }

    // 释放音频焦点
    fun abandonAudioFocus() {
        audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        audioFocusRequest = null
    }

    val audioPermission = rememberPermissionState(Manifest.permission.RECORD_AUDIO)
    LaunchedEffect(Unit) {
        if (!audioPermission.status.isGranted) audioPermission.launchPermissionRequest()
    }

    fun startRecording() {
        if (!audioPermission.status.isGranted || isRecording) return
        val file = File(context.cacheDir, "voice_${System.currentTimeMillis()}.m4a")// 生成临时文件
        outputFile = file
        val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }
        try {
            requestAudioFocus()
            recorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioSamplingRate(44100)
                setAudioEncodingBitRate(128000)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }
            mediaRecorder = recorder
            isRecording = true
            recordSeconds = 0
        } catch (_: Exception) {
            abandonAudioFocus()
            recorder.release()
        }
    }

    fun stopRecording(cancel: Boolean) {
        val recorder = mediaRecorder ?: return
        val file = outputFile ?: return
        try {
            recorder.stop()
        } catch (_: Exception) {}
        recorder.release()
        abandonAudioFocus()
        mediaRecorder = null
        isRecording = false
        isCancelling = false
        if (cancel ||  recordSeconds < 1) {
            file.delete()
            return
        }
        onVoiceRecorded(VoiceRecord(file,  recordSeconds))
    }

    // 释放资源
    DisposableEffect(Unit) {
        onDispose {
            mediaRecorder?.let { r ->
                try { r.stop() } catch (_: Exception) {}
                r.release()
            }
            abandonAudioFocus()
        }
    }

    // 焦点被抢占时自动停录
    LaunchedEffect(focusLost) {
        if (focusLost && isRecording) {
            focusLost = false
            stopRecording(cancel = false)
        }
    }

    // 录音计时
    LaunchedEffect(isRecording) {
        if (!isRecording) return@LaunchedEffect
        while (isRecording &&  recordSeconds < MAX_DURATION_SECONDS) {
            delay(1000L.milliseconds)
            recordSeconds++
        }
        if ( recordSeconds >= MAX_DURATION_SECONDS) stopRecording(cancel = false)
    }

    // 振幅采样
    LaunchedEffect(isRecording) {
        if (!isRecording) {
            currentAmplitude = 0f
            return@LaunchedEffect
        }
        while (isRecording) {
            val raw = mediaRecorder?.maxAmplitude ?: 0
            currentAmplitude = (raw / 12000f).coerceIn(0f, 1f)
            delay(AMPLITUDE_POLL_MS.milliseconds)
        }
    }

    // 录音面板与输入面板等高
    val sheetHeightDp = sheetTotalHeightDp.coerceAtLeast(400).dp

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }) {
                if (!isRecording) onDismiss()
            },
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(sheetHeightDp)
                .align(Alignment.BottomCenter)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }) {},
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            color = MaterialTheme.colorScheme.background,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .navigationBarsPadding()
                    .padding(bottom = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // 关闭按钮
                Box(modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, end = 8.dp)) {
                    IconButton(
                        onClick = {
                            stopRecording(cancel = true)
                            onDismiss()
                        },
                        modifier = Modifier.align(Alignment.TopEnd),
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = "关闭", tint = LimeGray)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 声波动画
                val lottieComposition by rememberLottieComposition(LottieCompositionSpec.Asset("lottie/soundwave.lottie"))
                // 声音驱动播放速度
                val lottieSpeed = if (isRecording) 0.5f + currentAmplitude * 0.7f else 1f
                val lottieProgress by animateLottieCompositionAsState(
                    composition = lottieComposition,
                    isPlaying = isRecording,
                    iterations = LottieConstants.IterateForever,
                    speed = lottieSpeed,
                )
                val waveColor = if (isRecording) Color.Black else LimeGray
                // 配置动画颜色
                val dynamicProperties = rememberLottieDynamicProperties(
                    rememberLottieDynamicProperty(LottieProperty.COLOR, waveColor.toArgb(), "**"),
                    rememberLottieDynamicProperty(LottieProperty.STROKE_COLOR, waveColor.toArgb(), "**"),
                )
                LottieAnimation(
                    composition = lottieComposition,
                    progress = { if (isRecording) lottieProgress else 0f },
                    dynamicProperties = dynamicProperties,
                    modifier = Modifier.size(160.dp, 72.dp),
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 提示文字
                Text(
                    text = when {
                        isCancelling -> "松开取消"
                        isRecording -> "${recordSeconds}\" · 松手保存"
                        else -> "按住麦克风开始录音"
                    },
                    fontSize = 14.sp,
                    color = if (isCancelling) Color(0xFFFF5252) else LimeGray,
                    fontWeight = if (isCancelling) FontWeight.Medium else FontWeight.Normal,
                )

                Spacer(modifier = Modifier.height(28.dp))

                Spacer(modifier = Modifier.weight(1f))

                // 时长上限提示
                if (isRecording) {
                    Text(
                        text = "${MAX_DURATION_SECONDS -  recordSeconds}s",
                        fontSize = 12.sp,
                        color = if ( recordSeconds >= 50) Color(0xFFFF5252) else LimeGray.copy(alpha = 0.6f),
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // 麦克风按钮（按住录音，上滑取消）
                var dragStartY by remember { mutableFloatStateOf(0f) }

                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(if (isRecording) LimePrimary else LimeLightGray)
                        .pointerInput(audioPermission.status.isGranted) {
                            fun vibrate() =
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            awaitPointerEventScope {
                                while (true) {
                                    // 等待按下
                                    val down = awaitPointerEvent(pass = PointerEventPass.Initial)
                                    val press = down.changes.firstOrNull { it.pressed }
                                    if (press == null) continue
                                    dragStartY = press.position.y// 记录按下瞬间的y轴
                                    vibrate()
                                    startRecording()

                                    // 追踪手指位移
                                    var cancel = false
                                    var wasCancelling = false
                                    while (true) {
                                        val move =
                                            awaitPointerEvent(pass = PointerEventPass.Initial)
                                        val current = move.changes.firstOrNull()
                                        if (current == null || !current.pressed) break
                                        val dy = dragStartY - current.position.y
                                        cancel = dy > 80f// 确认取消
                                        if (cancel && !wasCancelling) vibrate()
                                        wasCancelling = cancel
                                        isCancelling = cancel
                                    }
                                    stopRecording(cancel = cancel)
                                }
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Mic,
                        contentDescription = "录音",
                        tint = if (isRecording) Color.White else LimeGray,
                        modifier = Modifier.size(32.dp),
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}
