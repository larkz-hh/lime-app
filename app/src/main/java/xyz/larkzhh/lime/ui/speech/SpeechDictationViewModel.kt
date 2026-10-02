package xyz.larkzhh.lime.ui.speech

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.content.Context
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import xyz.larkzhh.lime.data.local.SpeechEngine
import xyz.larkzhh.lime.data.local.SpeechModelStore
import xyz.larkzhh.lime.data.local.SpeechPackStatus
import xyz.larkzhh.lime.data.local.SpeechPackUiState
import xyz.larkzhh.lime.work.SpeechPackDownloadWorker
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

/**
 * 语音转文字会话 ViewModel
 */
@HiltViewModel
class SpeechDictationViewModel @Inject constructor(
    private val engine: SpeechEngine,
    private val store: SpeechModelStore,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    /// 语音包状态
    val packState: StateFlow<SpeechPackUiState> = store.state

    private val _previewText = MutableStateFlow("")

    /// 实时预览文本
    val previewText: StateFlow<String> = _previewText.asStateFlow()

    @Volatile
    private var chunks: Channel<ByteArray> = Channel(Channel.UNLIMITED)
    private var consumerJob: Job? = null

    @Volatile
    private var finalText: String = ""

    @Volatile
    private var active: Boolean = false

    @Volatile
    private var discard: Boolean = false

    @Volatile
    private var consumerRunning: Boolean = false

    init {
        viewModelScope.launch { store.refresh() }
        viewModelScope.launch {
            store.state.collect { state ->
                if (state.status == SpeechPackStatus.Downloaded && active) launchConsumer()
            }
        }
    }

    /// 长按开始录音
    fun beginSession() {
        if (active) return
        // 上一次会话异常退出时复位
        if (consumerJob?.isActive != true) consumerRunning = false
        active = true
        discard = false
        finalText = ""
        _previewText.value = ""
        chunks = Channel(Channel.UNLIMITED)
        launchConsumer()
    }

    /// 采集回调
    fun feed(pcm: ByteArray) {
        if (!active) return
        chunks.trySend(pcm)
    }

    /// 进入转文字模式时调用
    fun ensureRecognition() {
        if (active) launchConsumer()
    }

    /// 松手结束，返回识别文本
    suspend fun finishRecognition(): String? {
        if (!active) return null
        active = false
        chunks.close()
        consumerJob?.let { job -> withTimeoutOrNull(FINISH_TIMEOUT_MS.milliseconds) { job.join() } }
        val text = finalText.trim().ifBlank { _previewText.value.trim() }
        finalText = ""
        _previewText.value = ""
        return text.ifBlank { null }
    }

    /// 取消
    fun cancelSession() {
        if (!active) return
        active = false
        discard = true
        finalText = ""
        chunks.close()
        _previewText.value = ""
    }

    /// 下载语音包
    fun downloadPack() {
        SpeechPackDownloadWorker.enqueue(context)
    }

    override fun onCleared() {
        active = false
        discard = true
        chunks.close()
        super.onCleared()
    }

    /// 启动消费
    private fun launchConsumer() {
        if (consumerRunning) return
        if (!engine.isReady()) return
        consumerRunning = true
        consumerJob = viewModelScope.launch(Dispatchers.Default) {
            val session = engine.openSession()
            if (session == null) {
                consumerRunning = false
                return@launch
            }
            try {
                for (chunk in chunks) {
                    _previewText.value = session.accept(chunk, chunk.size)
                }
                if (!discard) {
                    val result = session.finish().trim()
                    finalText = result.ifBlank { _previewText.value.trim() }
                    _previewText.value = finalText
                }
            } finally {
                session.close()
                consumerRunning = false
            }
        }
    }

    private companion object {
        const val FINISH_TIMEOUT_MS = 15_000L
    }
}
