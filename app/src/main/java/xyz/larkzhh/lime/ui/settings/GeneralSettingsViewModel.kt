package xyz.larkzhh.lime.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import xyz.larkzhh.lime.data.local.AppCacheManager
import xyz.larkzhh.lime.data.local.UserPreferences
import xyz.larkzhh.lime.data.notification.NotificationCenter
import javax.inject.Inject

/**
 * 通用设置 ViewModel
 */
@HiltViewModel
class GeneralSettingsViewModel @Inject constructor(
    private val preferences: UserPreferences,
    private val notificationCenter: NotificationCenter,
    private val cacheManager: AppCacheManager,
) : ViewModel() {

    /// 通知提醒总开关
    private val _notifyEnabled =
        MutableStateFlow(preferences.getBoolean(UserPreferences.Keys.NOTIFY_ENABLED, true))
    val notifyEnabled: StateFlow<Boolean> = _notifyEnabled.asStateFlow()

    /// 可清理缓存大小
    private val _cacheLabel = MutableStateFlow("")
    val cacheLabel: StateFlow<String> = _cacheLabel.asStateFlow()

    /// 切换通知提醒
    fun setNotifyEnabled(enabled: Boolean) {
        preferences.setBoolean(UserPreferences.Keys.NOTIFY_ENABLED, enabled)
        _notifyEnabled.value = enabled
        notificationCenter.onNotifyPreferenceChanged(enabled)
    }

    /// 刷新缓存大小
    fun refreshCacheSize() {
        viewModelScope.launch(Dispatchers.IO) {
            val label = cacheManager.cacheSizeLabel()
            withContext(Dispatchers.Main) { _cacheLabel.value = label }
        }
    }

    /// 清空缓存
    fun clearCache(onCleared: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            cacheManager.clearCache()
            val label = cacheManager.cacheSizeLabel()
            withContext(Dispatchers.Main) {
                _cacheLabel.value = label
                onCleared(label)
            }
        }
    }
}
