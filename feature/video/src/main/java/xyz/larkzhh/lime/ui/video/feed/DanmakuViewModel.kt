package xyz.larkzhh.lime.ui.video.feed

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import xyz.larkzhh.danmaku.DanmakuSelection
import xyz.larkzhh.lime.feature.video.R
import xyz.larkzhh.lime.data.network.model.DanmakuData
import xyz.larkzhh.lime.domain.repository.DanmakuRepository
import xyz.larkzhh.lime.domain.repository.UserRepository
import javax.inject.Inject

/// 某个视频已加载的弹幕窗口
data class DanmakuWindow(
    val fromMs: Long,
    val toMs: Long,
    val items: List<DanmakuData>,
)

/// 弹幕页面状态
data class DanmakuUiState(
    val currentNoteId: Long? = null,
    val danmakuByNote: Map<Long, DanmakuWindow> = emptyMap(),
    val enabled: Boolean = true,
    val color: String = "#FFFFFF",
    val showInput: Boolean = false,
    val selection: DanmakuSelection? = null,// 被点击冻结的弹幕
    val sendError: String? = null,
)

/// 窗口向前预取的时长
private const val WINDOW_FORWARD_MS = 60_000L

/// 窗口向后保留的时长
private const val WINDOW_BACK_MS = 15_000L

/// 重新拉取的提前量
private const val REFILL_AHEAD_MS = 15_000L

/**
 * 弹幕 ViewModel。
 *
 * 管理弹幕开关、弹幕发送、删除等操作
 */
@HiltViewModel
class DanmakuViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val danmakuRepository: DanmakuRepository,
    private val userRepository: UserRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DanmakuUiState())
    val uiState: StateFlow<DanmakuUiState> = _uiState.asStateFlow()

    /// 正在拉取的视频 id
    private val loadingNotes = mutableSetOf<Long>()

    /// 当前登录用户 id
    val currentUserId: Long? get() = userRepository.userFlow.value?.id

    /// 记录切到的视频
    fun setCurrentNote(noteId: Long) {
        _uiState.update { it.copy(currentNoteId = noteId) }
    }

    /// 播放位置推进
    fun onPlayheadMoved(noteId: Long, positionMs: Long) {
        val window = _uiState.value.danmakuByNote[noteId]
        if (window != null && positionMs >= window.fromMs && positionMs < window.toMs - REFILL_AHEAD_MS) return
        viewModelScope.launch { loadWindow(noteId, positionMs) }
    }

    /// 以当前播放位置为中心重新拉取弹幕窗口
    private suspend fun loadWindow(noteId: Long, positionMs: Long) {
        if (!loadingNotes.add(noteId)) return
        try {
            val from = (positionMs - WINDOW_BACK_MS).coerceAtLeast(0L)
            val to = positionMs + WINDOW_FORWARD_MS
            danmakuRepository.getDanmaku(noteId, from, to).onSuccess { resp ->
                _uiState.update {
                    it.copy(danmakuByNote = it.danmakuByNote + (noteId to DanmakuWindow(from, to, resp.items)))
                }
            }
        } finally {
            loadingNotes.remove(noteId)
        }
    }

    /// 发送弹幕
    fun sendDanmaku(noteId: Long, content: String, videoTimeMs: Long) {
        val text = content.trim()
        if (text.isEmpty()) return
        val color = _uiState.value.color
        viewModelScope.launch {
            danmakuRepository.postDanmaku(noteId, text, videoTimeMs, color).fold(
                onSuccess = { sent ->
                    _uiState.update { s ->
                        val window = s.danmakuByNote[noteId]
                        val updated = window
                            ?.copy(items = window.items + sent) ?: DanmakuWindow(videoTimeMs, videoTimeMs, listOf(sent))
                        s.copy(danmakuByNote = s.danmakuByNote + (noteId to updated))// 本地追加
                    }
                },
                onFailure = {
                    _uiState.update { it.copy(sendError = context.getString(R.string.video_danmaku_send_failed)) }
                },
            )
        }
    }

    /// 消费失败提示
    fun consumeSendError() = _uiState.update { it.copy(sendError = null) }

    /// 删除弹幕
    fun deleteDanmaku(noteId: Long, danmakuId: Long) {
        val before = _uiState.value.danmakuByNote[noteId] ?: return
        _uiState.update { s ->
            s.copy(
                danmakuByNote = s.danmakuByNote +
                    (noteId to before.copy(items = before.items.filterNot { it.id == danmakuId })),
            )
        }
        viewModelScope.launch {
            danmakuRepository.deleteDanmaku(noteId, danmakuId).onFailure {
                _uiState.update { s -> s.copy(danmakuByNote = s.danmakuByNote + (noteId to before)) }// 回滚
            }
        }
    }

    /// 弹幕开关
    fun toggleEnabled() {
        _uiState.update {
            val enabled = !it.enabled
            it.copy(enabled = enabled, showInput = if (enabled) it.showInput else false)
        }
    }

    /// 设置发送颜色
    fun setColor(hex: String) = _uiState.update { it.copy(color = hex) }

    /// 打开弹幕输入框，并开启弹幕
    fun openInput() = _uiState.update { it.copy(showInput = true, enabled = true) }

    /// 收起弹幕输入框
    fun closeInput() = _uiState.update { it.copy(showInput = false) }

    /// 选中弹幕变化
    fun onSelectionChange(selection: DanmakuSelection?) {
        _uiState.update { it.copy(selection = selection) }
    }

    /// 关闭气泡
    fun dismissBubble(): Boolean {
        val had = _uiState.value.selection != null
        if (had) _uiState.update { it.copy(selection = null) }
        return had
    }
}
