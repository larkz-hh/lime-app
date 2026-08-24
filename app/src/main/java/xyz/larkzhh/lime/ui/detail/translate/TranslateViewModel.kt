package xyz.larkzhh.lime.ui.detail.translate

import android.content.Context
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
import xyz.larkzhh.lime.data.local.TranslatorHolder
import xyz.larkzhh.lime.util.detectLanguageTag
import xyz.larkzhh.lime.work.TranslatePrefetchWorker
import javax.inject.Inject

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
    @ApplicationContext private val appContext: Context,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TranslateUiState())
    val uiState: StateFlow<TranslateUiState> = _uiState.asStateFlow()

    private val _fullText = MutableStateFlow(FullTextUiState())
    val fullText: StateFlow<FullTextUiState> = _fullText.asStateFlow()

    private var job: Job? = null
    private var fullTextJob: Job? = null

    /// 翻译选中文本
    fun translate(selectedText: String) {
        val text = selectedText.trim()
        if (text.isEmpty()) return
        translateWith(text, detectLanguageTag(text), allowAutoFlip = true)
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
        if (original.isNotEmpty()) translate(original)
    }

    /// 下载离线包
    fun scheduleBackgroundDownload() {
        TranslatePrefetchWorker.enqueueBackgroundDownload(appContext)
    }

    fun dismiss() {
        job?.cancel()
        _uiState.value = TranslateUiState()
    }

    private fun translateWith(text: String, source: String, allowAutoFlip: Boolean) {
        val target = if (source == "zh") "en" else "zh"
        job?.cancel()
        _uiState.value = TranslateUiState(
            visible = true,
            original = text,
            sourceTag = source,
            targetTag = target,
            phase = TranslatePhase.Downloading,
        )
        job = viewModelScope.launch {
            try {
                translatorHolder.ensureModel(source, target)
                _uiState.update { it.copy(phase = TranslatePhase.Translating) }
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
                        error = "翻译失败：首次使用需要联网下载语言包，请检查网络后重试",
                    )
                }
            }
        }
    }

    private data class TranslateOutcome(val result: String, val source: String, val target: String)

    /// 本地翻译，结果为空或等于原文时重翻
    private suspend fun translateSmart(
        text: String,
        source: String,
        allowAutoFlip: Boolean = true,
    ): TranslateOutcome {
        var src = source
        var tgt = if (source == "zh") "en" else "zh"
        translatorHolder.ensureModel(src, tgt)
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
}
