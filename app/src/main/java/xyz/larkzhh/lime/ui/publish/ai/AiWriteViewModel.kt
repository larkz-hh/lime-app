package xyz.larkzhh.lime.ui.publish.ai

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import xyz.larkzhh.lime.domain.model.AiWriteEvent
import xyz.larkzhh.lime.domain.repository.AiRepository
import xyz.larkzhh.lime.domain.repository.NoteRepository
import javax.inject.Inject

/// AI 帮写动作
enum class AiWriteAction(val code: String, val label: String) {
    CAPTION("caption", "看图写文案"),
    TITLE("title", "起标题"),
    POLISH("polish", "润色"),
    CONTINUE("continue", "续写"),
    CONDENSE("condense", "精简"),
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
) : ViewModel() {

    private val _state = MutableStateFlow(AiWriteUiState())
    val state: StateFlow<AiWriteUiState> = _state.asStateFlow()

    private var generateJob: Job? = null

    /// 图片上限
    private val maxImages = 4

    /// 开始动作
    fun start(action: AiWriteAction, content: String, imageUris: List<Uri>) {
        val trimmed = content.trim()
        val hasImages = imageUris.isNotEmpty()

        // 前置检验
        when (action) {
            AiWriteAction.CAPTION ->
                if (!hasImages) {
                    hint(action, "请先添加图片，才能看图写文案")
                    return
                }

            AiWriteAction.TITLE ->
                if (trimmed.isEmpty() && !hasImages) {
                    hint(action, "输入一点内容或添加图片后，再让 AI 起标题呗")
                    return
                }

            AiWriteAction.POLISH, AiWriteAction.CONTINUE, AiWriteAction.CONDENSE ->
                if (trimmed.length < MIN_TEXT_ACTION_CHARS) {
                    hint(action, "拜托，正文至少 $MIN_TEXT_ACTION_CHARS 个字，才能${action.label}")
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
                uploadImages(imageUris.take(maxImages)).getOrElse {
                    _state.update {
                        it.copy(isGenerating = false, isUploading = false, error = "图片上传失败，请重试")
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

    /// 逐张上传图片
    private suspend fun uploadImages(uris: List<Uri>): Result<List<String>> = runCatching {
        uris.mapIndexed { index, uri ->
            _state.update {
                it.copy(isUploading = true, uploadProgressText = "正在上传图片 ${index + 1}/${uris.size}…")
            }
            noteRepository.uploadImage(uri).getOrThrow()
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
