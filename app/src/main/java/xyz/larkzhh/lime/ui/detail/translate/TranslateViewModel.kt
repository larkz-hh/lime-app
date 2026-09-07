package xyz.larkzhh.lime.ui.detail.translate

import android.content.Context
import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import xyz.larkzhh.lime.R
import xyz.larkzhh.lime.data.local.TokenStorage
import xyz.larkzhh.lime.data.local.TranslateMode
import xyz.larkzhh.lime.data.local.TranslateSettings
import xyz.larkzhh.lime.data.local.TranslatorHolder
import xyz.larkzhh.lime.domain.repository.AiRepository
import xyz.larkzhh.lime.util.text.detectLanguageTag
import xyz.larkzhh.lime.work.TranslatePrefetchWorker
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

/// 翻译阶段
enum class TranslatePhase { Downloading, Translating, Done, Error }

data class TranslateUiState(
    val visible: Boolean = false,
    val original: String = "",
    val sourceTag: String = "zh",
    val targetTag: String = "en",
    val phase: TranslatePhase = TranslatePhase.Downloading,
    val result: String = "",
    val error: String? = null,
)

data class FullTextUiState(
    val translating: Boolean = false,
    val translated: Boolean = false,
    val translatedTitle: String? = null,
    val translatedContent: String? = null,
    val error: Boolean = false,
)

/**
 * 翻译 ViewModel
 * 翻译状态管理、选词翻译、一键翻译
 */
@HiltViewModel
class TranslateViewModel @Inject constructor(
    private val translatorHolder: TranslatorHolder,
    private val aiRepository: AiRepository,
    private val tokenStorage: TokenStorage,
    private val settings: TranslateSettings,
    @ApplicationContext private val appContext: Context,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TranslateUiState())
    val uiState: StateFlow<TranslateUiState> = _uiState.asStateFlow()

    private val _fullText = MutableStateFlow(FullTextUiState())
    val fullText: StateFlow<FullTextUiState> = _fullText.asStateFlow()

    private var job: Job? = null
    private var fullTextJob: Job? = null

    /// 去重防抖
    private var lastTranslateText: String? = null
    private var lastTranslateTime: Long = 0L

    /// 翻译选中文本
    fun translate(selectedText: String) {
        val text = selectedText.trim()
        if (text.isEmpty()) return
        val now = SystemClock.elapsedRealtime()
        if (text == lastTranslateText && now - lastTranslateTime < TRANSLATE_DEBOUNCE_MS) return
        lastTranslateText = text
        lastTranslateTime = now
        startTranslate(text)
    }

    /// 手动切换翻译方向后重译
    fun switchDirection() {
        val state = _uiState.value
        if (!state.visible || state.original.isEmpty()) return
        val flipped = if (state.sourceTag == "zh") "en" else "zh"
        translateWith(state.original, flipped, allowAutoFlip = false)
    }

    /// 一键翻译
    fun toggleFullTextTranslation(title: String?, content: String?) {
        val state = _fullText.value
        if (state.translated) {
            fullTextJob?.cancel()
            _fullText.value = FullTextUiState()
            return
        }
        if (state.translating) return
        fullTextJob?.cancel()
        _fullText.value = FullTextUiState(translating = true)
        fullTextJob = viewModelScope.launch {
            try {
                val translatedTitle = title?.takeIf { it.isNotBlank() }?.let {
                    translateSmart(it, detectLanguageTag(it)).result
                }
                val translatedContent = content?.takeIf { it.isNotBlank() }?.let {
                    translateSmart(it, detectLanguageTag(it)).result
                }
                _fullText.value = FullTextUiState(
                    translated = true,
                    translatedTitle = translatedTitle,
                    translatedContent = translatedContent,
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _fullText.value = FullTextUiState(error = true)
            }
        }
    }

    // 重试
    fun retry() {
        val original = _uiState.value.original
        if (original.isNotEmpty()) startTranslate(original)
    }

    /// 下载离线包
    fun scheduleBackgroundDownload() {
        TranslatePrefetchWorker.enqueueBackgroundDownload(appContext)
    }

    fun dismiss() {
        job?.cancel()
        _uiState.value = TranslateUiState()
    }

    /// 真正发起翻译（不走去重，供 retry 等主动重译使用）
    private fun startTranslate(text: String) {
        translateWith(text, detectLanguageTag(text), allowAutoFlip = true)
    }

    private fun translateWith(text: String, source: String, allowAutoFlip: Boolean) {
        val target = if (source == "zh") "en" else "zh"
        job?.cancel()
        _uiState.value = TranslateUiState(
            visible = true,
            original = text,
            sourceTag = source,
            targetTag = target,
            phase = TranslatePhase.Translating,
        )
        job = viewModelScope.launch {
            try {
                val outcome = translateSmart(text, source, allowAutoFlip)
                _uiState.update {
                    it.copy(
                        phase = TranslatePhase.Done,
                        result = outcome.result,
                        sourceTag = outcome.source,
                        targetTag = outcome.target,
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        phase = TranslatePhase.Error,
                        error = appContext.getString(R.string.translate_error_network),
                    )
                }
            }
        }
    }

    private data class TranslateOutcome(val result: String, val source: String, val target: String)

    /// 翻译入口，模式选择
    private suspend fun translateSmart(
        text: String,
        source: String,
        allowAutoFlip: Boolean = true,
    ): TranslateOutcome {
        return when (settings.mode.value) {
            TranslateMode.Offline -> offlineTranslate(text, source, allowAutoFlip)

            TranslateMode.Auto -> {
                aiTranslate(text, source)?.takeIf { it.isNotBlank() && it != text }?.let { ai ->
                    return TranslateOutcome(ai, source, if (source == "zh") "en" else "zh")
                }
                offlineTranslate(text, source, allowAutoFlip)
            }
        }
    }

    /// 离线翻译
    private suspend fun offlineTranslate(
        text: String,
        source: String,
        allowAutoFlip: Boolean,
    ): TranslateOutcome {
        var src = source
        var tgt = if (source == "zh") "en" else "zh"
        _uiState.update { it.copy(phase = TranslatePhase.Downloading) }
        translatorHolder.ensureModel(src, tgt)
        _uiState.update { it.copy(phase = TranslatePhase.Translating) }
        var result = translatorHolder.translate(text, src, tgt)
        if (allowAutoFlip && (result.isBlank() || result == text)) {
            val flippedSrc = tgt
            val flippedTgt = src
            translatorHolder.ensureModel(flippedSrc, flippedTgt)
            val flippedResult = translatorHolder.translate(text, flippedSrc, flippedTgt)
            // 翻面结果非空且不是原文
            if (flippedResult.isNotBlank() && flippedResult != text) {
                src = flippedSrc
                tgt = flippedTgt
                result = flippedResult
            }
        }
        return TranslateOutcome(result, src, tgt)
    }

    /// AI 翻译，未登录或失败降级离线
    private suspend fun aiTranslate(text: String, source: String): String? {
        if (!tokenStorage.isLoggedIn()) return null
        val targetLang = if (source == "zh") "英语" else "中文"
        val sourceLang = if (source == "zh") "中文" else "英语"
        // 8 秒内没出结果降级离线
        return withTimeoutOrNull(8_000.milliseconds) {
            aiRepository.translate(text, targetLang, sourceLang).getOrNull()
        }
    }

    private companion object {
        /// 相同文本翻译的去重窗口（毫秒），防止连点双发
        const val TRANSLATE_DEBOUNCE_MS = 600L
    }
}
