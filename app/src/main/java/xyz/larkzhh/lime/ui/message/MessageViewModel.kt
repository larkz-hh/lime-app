package xyz.larkzhh.lime.ui.message

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import xyz.larkzhh.lime.domain.model.NotificationCategory
import xyz.larkzhh.lime.domain.repository.NotificationRepository
import javax.inject.Inject

/**
 * 消息页 ViewModel
 */
@HiltViewModel
class MessageViewModel @Inject constructor(
    private val notificationRepository: NotificationRepository,
) : ViewModel() {

    // 各入口未读数
    val unreadByCategory: StateFlow<Map<NotificationCategory, Int>> =
        notificationRepository.unreadByCategory

    // 底部 Tab 总未读数
    val totalUnread: StateFlow<Int> = notificationRepository.totalUnread

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
