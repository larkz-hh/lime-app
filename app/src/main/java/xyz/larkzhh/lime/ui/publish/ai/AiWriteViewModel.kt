package xyz.larkzhh.lime.ui.publish.ai

import android.content.Context
import android.net.Uri
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import xyz.larkzhh.lime.R
import xyz.larkzhh.lime.domain.model.AiWriteEvent
import xyz.larkzhh.lime.domain.repository.AiRepository
import xyz.larkzhh.lime.domain.repository.NoteRepository
import javax.inject.Inject

/// AI 可用图
data class AiWriteImage(
    val uri: Uri? = null,
    val remoteUrl: String? = null,
)

/// AI 帮写动作
enum class AiWriteAction(val code: String, @StringRes val labelRes: Int) {
    CAPTION("caption", R.string.ai_write_action_caption),
    TITLE("title", R.string.ai_write_action_title),
    POLISH("polish", R.string.ai_write_action_polish),
    CONTINUE("continue", R.string.ai_write_action_continue),
    CONDENSE("condense", R.string.ai_write_action_condense),
    ;

    companion object {
        /// 按是否有图决定展示顺序
        fun visibleFor(hasImages: Boolean): List<AiWriteAction> =
            if (hasImages) listOf(CAPTION, TITLE, POLISH, CONTINUE, CONDENSE)
            else listOf(POLISH, TITLE, CONTINUE, CONDENSE)
        /// 打开面板时默认动作
        fun defaultFor(hasImages: Boolean): AiWriteAction =
            if (hasImages) CAPTION else POLISH
    }
}

/// 润色、续写、精简要求正文最短字数
const val MIN_TEXT_ACTION_CHARS = 10

data class AiWriteUiState(
    val action: AiWriteAction = AiWriteAction.POLISH,
    val isGenerating: Boolean = false,
    val isUploading: Boolean = false,
    val uploadProgressText: String? = null,
    val text: String = "",                 // 流式累计文本
    val titles: List<String> = emptyList(), // 候选标题
    val error: String? = null,
    val finished: Boolean = false,
)

/**
 * 发布页 AI 帮写面板 ViewModel
 * - AI 阿奎那图生文案、起标题、润色、续写、精简
 */
@HiltViewModel
class AiWriteViewModel @Inject constructor(
    private val aiRepository: AiRepository,
    private val noteRepository: NoteRepository,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _state = MutableStateFlow(AiWriteUiState())
    val state: StateFlow<AiWriteUiState> = _state.asStateFlow()

    private var generateJob: Job? = null

    /// 图片上限
    private val maxImages = 4

    /// 开始动作
    fun start(action: AiWriteAction, content: String, images: List<AiWriteImage>) {
        val trimmed = content.trim()
        val hasImages = images.isNotEmpty()

        // 前置检验
        when (action) {
            AiWriteAction.CAPTION ->
                if (!hasImages) {
                    hint(action, context.getString(R.string.ai_write_hint_caption_need_image))
                    return
                }

            AiWriteAction.TITLE ->
                if (trimmed.isEmpty() && !hasImages) {
                    hint(action, context.getString(R.string.ai_write_hint_title_need_input))
                    return
                }

            AiWriteAction.POLISH, AiWriteAction.CONTINUE, AiWriteAction.CONDENSE ->
                if (trimmed.length < MIN_TEXT_ACTION_CHARS) {
                    val message = when (action) {
                        AiWriteAction.CONTINUE ->
                            context.getString(R.string.ai_write_min_text_continue, MIN_TEXT_ACTION_CHARS)
                        AiWriteAction.CONDENSE ->
                            context.getString(R.string.ai_write_min_text_condense, MIN_TEXT_ACTION_CHARS)
                        else ->
                            context.getString(R.string.ai_write_min_text_polish, MIN_TEXT_ACTION_CHARS)
                    }
                    hint(action, message)
                    return
                }
        }

        val current = _state.value
        if (current.isGenerating && current.action == action) return
        generateJob?.cancel()
        generateJob = viewModelScope.launch {
            _state.update { AiWriteUiState(action = action, isGenerating = true) }

            // 看图写文案必须带图，起标题有图时带图
            val needImages = action == AiWriteAction.CAPTION ||
                (action == AiWriteAction.TITLE && hasImages)
            val imageUrls = if (needImages) {
                resolveImageUrls(images).getOrElse {
                    _state.update {
                        it.copy(
                            isGenerating = false,
                            isUploading = false,
                            error = context.getString(R.string.ai_write_error_upload_failed),
                        )
                    }
                    return@launch
                }
            } else {
                emptyList()
            }

            _state.update { it.copy(isUploading = false, uploadProgressText = null) }
            aiRepository.writeAssist(
                action = action.code,
                content = trimmed.ifBlank { null }?.take(2000),
                imageUrls = imageUrls.ifEmpty { null },
            ).collect { event ->
                when (event) {
                    is AiWriteEvent.Delta ->
                        _state.update { it.copy(text = it.text + event.content) }

                    is AiWriteEvent.Done -> {
                        val finalText = event.content.ifBlank { _state.value.text }
                        val titles =
                            if (action == AiWriteAction.TITLE) parseTitles(finalText) else emptyList()
                        _state.update {
                            it.copy(
                                text = finalText,
                                titles = titles,
                                isGenerating = false,
                                finished = true,
                                error = null,
                            )
                        }
                    }

                    is AiWriteEvent.Error ->
                        _state.update { it.copy(isGenerating = false, error = event.message) }
                }
            }
        }
    }

    /// 停止生成，断开连接
    fun stop() {
        generateJob?.cancel()
        generateJob = null
        _state.update { it.copy(isGenerating = false, isUploading = false) }
    }

    override fun onCleared() {
        generateJob?.cancel()
        super.onCleared()
    }

    /// 面板内提示
    private fun hint(action: AiWriteAction, message: String) {
        _state.update {
            it.copy(
                action = action,
                error = message,
                text = "",
                titles = emptyList(),
                isGenerating = false,
                isUploading = false,
                finished = false,
            )
        }
    }

    /// 拼接 AI 需要的图片 URL
    private suspend fun resolveImageUrls(images: List<AiWriteImage>): Result<List<String>> = runCatching {
        val chosen = images.filter { it.uri != null || it.remoteUrl != null }.take(maxImages)
        val localCount = chosen.count { it.uri != null }
        var uploaded = 0
        chosen.map { image ->
            if (image.uri != null) {
                uploaded++
                _state.update {
                    it.copy(
                        isUploading = true,
                        uploadProgressText = context.getString(
                            R.string.ai_write_uploading_progress, uploaded, localCount
                        ),
                    )
                }
                noteRepository.uploadImage(image.uri).getOrThrow()
            } else {
                image.remoteUrl ?: error("图片缺少地址")
            }
        }
    }

    /// 拆分起标题结果
    private fun parseTitles(text: String): List<String> =
        text.lines()
            .map { it.trim().replace(TITLE_PREFIX, "") }
            .filter { it.isNotBlank() }
            .take(3)

    private companion object {
        val TITLE_PREFIX = Regex("^\\d+[.、)）:：]\\s*")
    }
}
