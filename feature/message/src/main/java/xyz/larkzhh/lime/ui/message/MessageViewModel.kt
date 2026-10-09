package xyz.larkzhh.lime.ui.message

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import xyz.larkzhh.lime.domain.model.NotificationCategory
import xyz.larkzhh.lime.domain.repository.ImRepository
import xyz.larkzhh.lime.domain.repository.NotificationRepository
import javax.inject.Inject

/**
 * 消息页 ViewModel
 */
@HiltViewModel
class MessageViewModel @Inject constructor(
    private val notificationRepository: NotificationRepository,
    private val imRepository: ImRepository,
) : ViewModel() {

    // 各入口未读数
    val unreadByCategory: StateFlow<Map<NotificationCategory, Int>> =
        notificationRepository.unreadByCategory

    // 站内通知总未读数
    val totalUnread: StateFlow<Int> = notificationRepository.totalUnread

    // IM 会话未读数
    private val _imUnread = MutableStateFlow(0)

    // 底部 Tab 红点合计
    val combinedUnread: StateFlow<Int> =
        combine(notificationRepository.totalUnread, _imUnread) { inbox, im -> inbox + im }
            .stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    init {
        viewModelScope.launch {
            imRepository.conversationUnreadFlow.collect { _imUnread.value = it }
        }
    }

    /// 登录下同步未读、实时推送
    fun sync() {
        viewModelScope.launch { notificationRepository.refresh() }
        notificationRepository.startSse()
    }

    /// 登出时停止推送
    fun onLoggedOut() {
        notificationRepository.stopSse()
    }

    override fun onCleared() {
        notificationRepository.stopSse()
        super.onCleared()
    }
}
