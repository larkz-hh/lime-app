package xyz.larkzhh.lime.ui.settings

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import xyz.larkzhh.lime.data.local.UserPreferences
import xyz.larkzhh.lime.data.notify.NotificationCenter
import javax.inject.Inject

/**
 * 通用设置 ViewModel
 */
@HiltViewModel
class GeneralSettingsViewModel @Inject constructor(
    private val preferences: UserPreferences,
    private val notificationCenter: NotificationCenter,
) : ViewModel() {

    /// 通知提醒总开关
    private val _notifyEnabled =
        MutableStateFlow(preferences.getBoolean(UserPreferences.Keys.NOTIFY_ENABLED, true))
    val notifyEnabled: StateFlow<Boolean> = _notifyEnabled.asStateFlow()

    /// 切换通知提醒
    fun setNotifyEnabled(enabled: Boolean) {
        preferences.setBoolean(UserPreferences.Keys.NOTIFY_ENABLED, enabled)
        _notifyEnabled.value = enabled
        notificationCenter.onNotifyPreferenceChanged(enabled)
    }
}
