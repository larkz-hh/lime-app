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
import xyz.larkzhh.lime.R
import xyz.larkzhh.lime.data.network.model.DanmakuData
import xyz.larkzhh.lime.domain.repository.DanmakuRepository
import xyz.larkzhh.lime.domain.repository.UserRepository
import javax.inject.Inject

/// 弹幕页面状态
data class DanmakuUiState(
    val currentNoteId: Long? = null,
    val danmakuByNote: Map<Long, List<DanmakuData>> = emptyMap(),
    val enabled: Boolean = true,
    val color: String = "#FFFFFF",
    val showInput: Boolean = false,
    val pausedDanmakuId: Long? = null,// 被点击冻结的弹幕
    val frozenMs: Long = 0L,// 冻结时播放进度
    val sendError: String? = null,
)

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

    /// 当前登录用户 id
    val currentUserId: Long? get() = userRepository.userFlow.value?.id

    /// 记录切到的视频，加载弹幕
    fun setCurrentNote(noteId: Long) {
        _uiState.update { it.copy(currentNoteId = noteId) }
        loadDanmaku(noteId)
    }

    /// 拉取某视频弹幕）
    private fun loadDanmaku(noteId: Long) {
        if (_uiState.value.danmakuByNote.containsKey(noteId)) return
        viewModelScope.launch {
            danmakuRepository.getDanmaku(noteId).onSuccess { resp ->
                _uiState.update { it.copy(danmakuByNote = it.danmakuByNote + (noteId to resp.items)) }
            }
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
                        val list = s.danmakuByNote[noteId].orEmpty() + sent// 本地追加
                        s.copy(danmakuByNote = s.danmakuByNote + (noteId to list))
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
        val before = _uiState.value.danmakuByNote[noteId].orEmpty()
        _uiState.update { s ->
            s.copy(danmakuByNote = s.danmakuByNote + (noteId to before.filterNot { it.id == danmakuId }))
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

    /// 点击某条弹幕，打开气泡
    fun onDanmakuClick(danmakuId: Long, nowMs: Long) {
        _uiState.update {
            if (it.pausedDanmakuId == danmakuId) {
                it.copy(pausedDanmakuId = null)
            } else {
                it.copy(pausedDanmakuId = danmakuId, frozenMs = nowMs)
            }
        }
    }

    /// 关闭气泡
    fun dismissBubble(): Boolean {
        val had = _uiState.value.pausedDanmakuId != null
        if (had) _uiState.update { it.copy(pausedDanmakuId = null) }
        return had
    }
}
