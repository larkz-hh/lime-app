package xyz.larkzhh.lime.ui.im.viewmodel

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
import xyz.larkzhh.lime.domain.repository.ImRepository
import javax.inject.Inject

data class ImUiState(
    val isLoggingIn: Boolean = false,
    val isLoggedIn: Boolean = false,
    val errorMessage: String? = null,
)

/**
 * IM ViewModel。
 */
@HiltViewModel
class ImViewModel @Inject constructor(
    private val imRepository: ImRepository,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _state = MutableStateFlow(ImUiState())
    val state: StateFlow<ImUiState> = _state.asStateFlow()

    /// 登录成功后调用，确保 IM 已登录
    fun ensureImLogin() {
        viewModelScope.launch {
            _state.update { it.copy(isLoggingIn = true, errorMessage = null) }
            imRepository.ensureImLogin().fold(
                onSuccess = { _state.update { it.copy(isLoggingIn = false, isLoggedIn = true) } },
                onFailure = { e ->
                    _state.update { it.copy(isLoggingIn = false, errorMessage = e.message ?: context.getString(R.string.im_login_failed)) }
                },
            )
        }
    }

    /// 打开私信会话
    fun openConversation(targetUserId: Long, onOpened: (String) -> Unit) {
        viewModelScope.launch {
            _state.update { it.copy(errorMessage = null) }
            imRepository.openConversation(targetUserId).fold(
                onSuccess = onOpened,
                onFailure = { e ->
                    _state.update { it.copy(errorMessage = e.message ?: context.getString(R.string.im_open_conversation_failed)) }
                },
            )
        }
    }

    /// 清除错误提示
    fun clearError() = _state.update { it.copy(errorMessage = null) }

    /// IM 登出
    fun logout() {
        viewModelScope.launch { imRepository.logout() }
    }
}
