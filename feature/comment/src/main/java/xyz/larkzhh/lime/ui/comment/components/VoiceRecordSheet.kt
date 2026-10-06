package xyz.larkzhh.lime.ui.comment.components

import android.Manifest
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.KeyboardVoice
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import xyz.larkzhh.lime.core.designsystem.R as DesignSystemR
import xyz.larkzhh.lime.data.local.SpeechEngine
import xyz.larkzhh.lime.data.local.SpeechPackStatus
import xyz.larkzhh.lime.ui.components.SoundWaveAnimation
import xyz.larkzhh.swipeback.blockPageSwipe
import xyz.larkzhh.lime.ui.comment.viewmodel.VoiceRecord
import xyz.larkzhh.lime.ui.speech.SpeechDictationViewModel
import xyz.larkzhh.lime.ui.theme.LimeGray
import xyz.larkzhh.lime.util.audio.M4aEncoder
import xyz.larkzhh.lime.util.audio.PcmRecorder
import xyz.larkzhh.lime.util.showToast
import java.io.File
import kotlin.time.Duration.Companion.milliseconds

private const val MAX_DURATION_SECONDS = 60// 最大录音时长
private const val AMPLITUDE_POLL_MS = 80L// 振幅采样轮询时间间隔
private const val MIC_SIZE_DP = 72// 麦克风按钮尺寸
private const val MIC_AREA_WIDTH_DP = 204
private const val MIC_AREA_HEIGHT_DP = 112
private const val STT_ENTER_X = 90f// 右上角转文字进入阈值
private const val STT_ENTER_Y = 40f
private const val STT_KEEP_X = 50f// 回退到录音的滞后阈值
private const val STT_KEEP_Y = 15f
private const val CANCEL_X = -40f// 左上角取消向左阈值
private const val CANCEL_Y = 70f// 左上角取消向上阈值

/// 手势模式
private enum class VoiceMode { Recording, Transcribing }

/**
 * 录音浮层
 *
 * @param sheetTotalHeightDp 输入面板总高度
 * @param onVoiceRecorded 录音完成回调
 * @param onTextRecognized 语音转文字完成回调
 * @param onDismiss 关闭回调
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun VoiceRecordSheet(
    sheetTotalHeightDp: Int = 0,
    onVoiceRecorded: (VoiceRecord) -> Unit,
    onTextRecognized: (String) -> Unit = {},
    onDismiss: () -> Unit,
    dictationViewModel: SpeechDictationViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val previewText by dictationViewModel.previewText.collectAsState()
    val packState by dictationViewModel.packState.collectAsState()
    val recordFailedText = stringResource(DesignSystemR.string.voice_record_failed)
    val transcribeEmptyText = stringResource(DesignSystemR.string.voice_transcribe_empty)
    val packMissingText = stringResource(DesignSystemR.string.voice_pack_missing)

    var isRecording by remember { mutableStateOf(false) }
    var recordSeconds by remember { mutableIntStateOf(0) }// 录音已持续的秒数
    var isCancelling by remember { mutableStateOf(false) }// 左上角取消提示
    var isBusy by remember { mutableStateOf(false) }// 生成语音 / 识别文本中
    var mode by remember { mutableStateOf(VoiceMode.Recording) }
    var currentAmplitude by remember { mutableFloatStateOf(0f) }

    // PCM 采集
    val recorder = remember { PcmRecorder(SpeechEngine.SAMPLE_RATE) }
    var sessionActive by remember { mutableStateOf(false) }

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

    /// 按住开始采集
    fun beginCapture() {
        if (sessionActive) return
        if (!audioPermission.status.isGranted) return
        requestAudioFocus()
        dictationViewModel.beginSession()
        recorder.onChunk = { chunk -> dictationViewModel.feed(chunk) }
        if (!recorder.start()) {
            abandonAudioFocus()
            dictationViewModel.cancelSession()
            recordFailedText.showToast(context)
            return
        }
        sessionActive = true
        isRecording = true
        isCancelling = false
        recordSeconds = 0
        mode = VoiceMode.Recording
    }

    /// 松手
    fun finishCapture(cancel: Boolean, toText: Boolean) {
        if (!sessionActive) return
        sessionActive = false
        isRecording = false
        isCancelling = false
        val pcm = recorder.stop()
        abandonAudioFocus()
        val seconds = recorder.recordedSeconds
        when {
            cancel -> dictationViewModel.cancelSession()

            toText -> scope.launch {
                isBusy = true
                val text = dictationViewModel.finishRecognition()
                isBusy = false
                if (text.isNullOrBlank()) {
                    val message = if (dictationViewModel.packState.value.status == SpeechPackStatus.Downloaded) {
                        transcribeEmptyText
                    } else {
                        packMissingText
                    }
                    message.showToast(context)
                } else {
                    onTextRecognized(text)
                }
            }

            else -> {
                dictationViewModel.cancelSession()
                if (seconds < 1) return
                scope.launch {
                    isBusy = true
                    val file = File(context.cacheDir, "voice_${System.currentTimeMillis()}.m4a")
                    val success = M4aEncoder.encode(
                        pcm = pcm,
                        sampleRate = SpeechEngine.SAMPLE_RATE,
                        channelCount = 1,
                        outputFile = file,
                    )
                    isBusy = false
                    if (success) {
                        onVoiceRecorded(VoiceRecord(file, seconds))
                    } else {
                        file.delete()
                        recordFailedText.showToast(context)
                    }
                }
            }
        }
    }

    /// 进入或退出转文字模式
    fun switchMode(transcribing: Boolean) {
        mode = if (transcribing) VoiceMode.Transcribing else VoiceMode.Recording
        if (!transcribing) return
        dictationViewModel.ensureRecognition()
        // 语音包缺失时下载
        val status = dictationViewModel.packState.value.status
        if (status == SpeechPackStatus.NotDownloaded || status == SpeechPackStatus.Failed) {
            dictationViewModel.downloadPack()
        }
    }

    // 释放资源
    DisposableEffect(Unit) {
        onDispose {
            recorder.stop()
            abandonAudioFocus()
            dictationViewModel.cancelSession()
        }
    }

    // 焦点被抢占时自动停录
    LaunchedEffect(focusLost) {
        if (focusLost && isRecording) {
            focusLost = false
            finishCapture(cancel = false, toText = mode == VoiceMode.Transcribing)
        }
    }

    // 录音计时
    LaunchedEffect(isRecording) {
        if (!isRecording) return@LaunchedEffect
        while (isRecording && recordSeconds < MAX_DURATION_SECONDS) {
            delay(1000L.milliseconds)
            recordSeconds++
        }
        if (recordSeconds >= MAX_DURATION_SECONDS) {
            finishCapture(cancel = false, toText = mode == VoiceMode.Transcribing)
        }
    }

    // 振幅采样
    LaunchedEffect(isRecording) {
        if (!isRecording) {
            currentAmplitude = 0f
            return@LaunchedEffect
        }
        while (isRecording) {
            currentAmplitude = recorder.amplitude
            delay(AMPLITUDE_POLL_MS.milliseconds)
        }
    }

    // 录音面板与输入面板等高
    val sheetHeightDp = sheetTotalHeightDp.coerceAtLeast(400).dp

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f))
            .blockPageSwipe()
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }) {
                if (!isRecording && !isBusy) onDismiss()
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
                // 顶部关闭按钮
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .padding(horizontal = 16.dp),
                ) {
                    if (!isRecording) {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.align(Alignment.TopEnd),
                        ) {
                            Icon(
                                Icons.Filled.Close,
                                contentDescription = stringResource(DesignSystemR.string.voice_close),
                                tint = LimeGray,
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                if (mode == VoiceMode.Transcribing) {
                    // 转文字预览
                    TranscribePreview(
                        text = previewText,
                        status = packState.status,
                        progress = packState.progress,
                        onDownload = dictationViewModel::downloadPack,
                    )
                } else {
                    // 声波动画
                    SoundWaveAnimation(
                        isRecording = isRecording,
                        amplitude = currentAmplitude,
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 提示文字
                val hint = when {
                    isBusy -> stringResource(DesignSystemR.string.voice_processing)
                    mode == VoiceMode.Transcribing -> stringResource(DesignSystemR.string.voice_release_to_text)
                    isCancelling -> stringResource(DesignSystemR.string.voice_release_cancel)
                    isRecording -> stringResource(DesignSystemR.string.voice_release_send, recordSeconds)
                    else -> stringResource(DesignSystemR.string.voice_hold_to_record)
                }
                Text(
                    text = hint,
                    fontSize = 14.sp,
                    color = if (isCancelling) Color(0xFFFF5252) else LimeGray,
                    fontWeight = if (isCancelling) FontWeight.Medium else FontWeight.Normal,
                )

                Spacer(modifier = Modifier.height(12.dp))

                Spacer(modifier = Modifier.weight(1f))

                // 时长上限提示
                if (isRecording) {
                    Text(
                        text = "${MAX_DURATION_SECONDS - recordSeconds}s",
                        fontSize = 12.sp,
                        color = if (recordSeconds >= 50) Color(0xFFFF5252) else LimeGray.copy(alpha = 0.6f),
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // 麦克风按钮
                var dragStartX by remember { mutableFloatStateOf(0f) }
                var dragStartY by remember { mutableFloatStateOf(0f) }

                Box(
                    modifier = Modifier
                        .width(MIC_AREA_WIDTH_DP.dp)
                        .height(MIC_AREA_HEIGHT_DP.dp),
                ) {
                    // 左上角取消
                    if (isRecording) {
                        GestureSideButton(
                            icon = Icons.Filled.Close,
                            label = stringResource(DesignSystemR.string.cancel),
                            active = isCancelling,
                            activeColor = Color(0xFFFF5252),
                            modifier = Modifier.align(Alignment.TopStart),
                        )
                    }
                    // 右上角转文字
                    if (isRecording) {
                        GestureSideButton(
                            icon = Icons.Outlined.KeyboardVoice,
                            label = stringResource(DesignSystemR.string.voice_to_text),
                            active = mode == VoiceMode.Transcribing,
                            activeColor = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.align(Alignment.TopEnd),
                        )
                    }

                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .size(MIC_SIZE_DP.dp)
                            .clip(CircleShape)
                            .background(if (isRecording) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                            .pointerInput(audioPermission.status.isGranted) {
                                fun vibrate() =
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                awaitPointerEventScope {
                                    while (true) {
                                        // 等待按下
                                        val down = awaitPointerEvent(pass = PointerEventPass.Initial)
                                        val press = down.changes.firstOrNull { it.pressed }
                                        if (press == null) continue
                                        dragStartX = press.position.x
                                        dragStartY = press.position.y
                                        vibrate()
                                        beginCapture()

                                        // 追踪手指位移
                                        var transcribing = false
                                        var cancel = false
                                        var wasTranscribing = false
                                        var wasCancelling = false
                                        while (true) {
                                            val move = awaitPointerEvent(pass = PointerEventPass.Initial)
                                            val current = move.changes.firstOrNull()
                                            if (current == null || !current.pressed) break
                                            val dx = current.position.x - dragStartX
                                            val dy = dragStartY - current.position.y// 向上为正
                                            // 右上角进入转文字
                                            transcribing = if (transcribing) {
                                                dx >= STT_KEEP_X && dy >= STT_KEEP_Y
                                            } else {
                                                dx >= STT_ENTER_X && dy >= STT_ENTER_Y
                                            }
                                            if (transcribing != wasTranscribing) {
                                                wasTranscribing = transcribing
                                                vibrate()
                                                switchMode(transcribing)
                                            }
                                            // 左上角松手取消
                                            cancel = !transcribing && dy >= CANCEL_Y && dx <= CANCEL_X
                                            if (cancel && !wasCancelling) vibrate()
                                            wasCancelling = cancel
                                            isCancelling = cancel
                                        }
                                        finishCapture(cancel = cancel, toText = transcribing)
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Mic,
                            contentDescription = stringResource(DesignSystemR.string.voice_record),
                            tint = if (isRecording) Color.White else LimeGray,
                            modifier = Modifier.size(32.dp),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

/// 左上取消 / 右上转文字
@Composable
private fun GestureSideButton(
    icon: ImageVector,
    label: String,
    active: Boolean,
    activeColor: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(if (active) activeColor else MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (active) Color.White else LimeGray,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            color = if (active) activeColor else LimeGray,
        )
    }
}
