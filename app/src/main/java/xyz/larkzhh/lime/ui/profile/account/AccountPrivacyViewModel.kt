package xyz.larkzhh.lime.ui.profile.account

import android.content.Context
import android.util.Patterns
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import xyz.larkzhh.lime.R
import xyz.larkzhh.lime.domain.repository.AuthRepository
import xyz.larkzhh.lime.domain.repository.UserRepository
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

/// 身份验证方式
enum class VerifyMode(@StringRes val labelRes: Int) {
    OldPassword(R.string.account_verify_old_password),
    Code(R.string.account_verify_email_code),
}

data class AccountPrivacyUiState(
    val email: String? = null,
    val likePrivate: Boolean = false,
    val favPrivate: Boolean = false,
    val isPrivacySaving: Boolean = false,
    val privacyError: String? = null,
    val verifyMode: VerifyMode = VerifyMode.OldPassword,
    val oldPassword: String = "",
    val code: String = "",
    val newPassword: String = "",
    val confirmPassword: String = "",
    val passwordVisible: Boolean = false,
    val sendCodeCountdown: Int = 0,
    val isSendingCode: Boolean = false,
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
    val success: Boolean = false,
)

/**
 * 账号与隐私页 ViewModel
 */
@HiltViewModel
class AccountPrivacyViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AccountPrivacyUiState(email = userRepository.userFlow.value?.email))
    val uiState: StateFlow<AccountPrivacyUiState> = _uiState.asStateFlow()

    private var countdownJob: Job? = null

    /// 重置表单与状态，刷新邮箱与隐私开关
    fun reset() {
        countdownJob?.cancel()
        val cached = userRepository.userFlow.value
        _uiState.update {
            AccountPrivacyUiState(
                email = cached?.email,
                likePrivate = cached?.likePrivate ?: false,
                favPrivate = cached?.favPrivate ?: false,
            )
        }
        if (cached == null) {
            viewModelScope.launch {
                userRepository.refreshUser()
                    .onSuccess { user ->
                        _uiState.update {
                            it.copy(
                                email = user.email,
                                likePrivate = user.likePrivate,
                                favPrivate = user.favPrivate,
                            )
                        }
                    }
            }
        }
    }

    /// 切换点赞列表隐私
    fun setLikePrivate(enabled: Boolean) {
        val s = _uiState.value
        updatePrivacy(enabled, s.favPrivate)
    }

    /// 切换收藏列表隐私
    fun setFavPrivate(enabled: Boolean) {
        val s = _uiState.value
        updatePrivacy(s.likePrivate, enabled)
    }

    /// 清除隐私设置错误提示
    fun clearPrivacyError() = _uiState.update { it.copy(privacyError = null) }

    /// 退出登录
    fun logout(onDone: () -> Unit) {
        viewModelScope.launch {
            runCatching { authRepository.logout() }
            userRepository.clearActiveSession()
            _uiState.value = AccountPrivacyUiState()
            onDone()
        }
    }

    /// 更新隐私
    private fun updatePrivacy(likePrivate: Boolean, favPrivate: Boolean) {
        val state = _uiState.value
        if (state.isPrivacySaving) return
        val prevLike = state.likePrivate
        val prevFav = state.favPrivate
        // 乐观更新 UI，失败回滚
        _uiState.update {
            it.copy(isPrivacySaving = true, likePrivate = likePrivate, favPrivate = favPrivate)
        }
        viewModelScope.launch {
            try {
                val user = userRepository.updatePrivacy(likePrivate, favPrivate).getOrThrow()
                _uiState.update {
                    it.copy(
                        isPrivacySaving = false,
                        likePrivate = user.likePrivate,
                        favPrivate = user.favPrivate,
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isPrivacySaving = false,
                        likePrivate = prevLike,
                        favPrivate = prevFav,
                        privacyError = e.message ?: context.getString(R.string.account_privacy_save_failed),
                    )
                }
            }
        }
    }

    /// 切换验证方式
    fun setVerifyMode(mode: VerifyMode) =
        _uiState.update { it.copy(verifyMode = mode, errorMessage = null) }

    fun onOldPasswordChange(value: String) =
        _uiState.update { it.copy(oldPassword = value, errorMessage = null) }

    fun onCodeChange(value: String) =
        _uiState.update { it.copy(code = value, errorMessage = null) }

    fun onNewPasswordChange(value: String) =
        _uiState.update { it.copy(newPassword = value, errorMessage = null) }

    fun onConfirmPasswordChange(value: String) =
        _uiState.update { it.copy(confirmPassword = value, errorMessage = null) }

    /// 切换新密码明文、密文显示
    fun togglePasswordVisible() =
        _uiState.update { it.copy(passwordVisible = !it.passwordVisible) }

    /// 向绑定邮箱发送验证码
    fun sendCode() {
        val s = _uiState.value
        val email = s.email
        if (email.isNullOrBlank() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _uiState.update { it.copy(errorMessage = context.getString(R.string.account_code_send_no_email)) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSendingCode = true, errorMessage = null) }
            try {
                authRepository.sendCode(email).getOrThrow()
                startCountdown()
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message ?: context.getString(R.string.account_code_send_failed)) }
            } finally {
                _uiState.update { it.copy(isSendingCode = false) }
            }
        }
    }

    /// 消费修改成功标志
    fun clearSuccess() = _uiState.update { it.copy(success = false) }

    /// 提交修改密码
    fun submitChangePassword() {
        val s = _uiState.value
        val error = when {
            s.verifyMode == VerifyMode.OldPassword && s.oldPassword.isBlank() -> context.getString(R.string.account_enter_current_password)
            s.verifyMode == VerifyMode.Code && s.code.isBlank() -> context.getString(R.string.account_enter_email_code)
            !isValidNewPassword(s.newPassword) -> context.getString(R.string.account_password_rule)
            s.verifyMode == VerifyMode.OldPassword && s.newPassword == s.oldPassword -> context.getString(R.string.account_password_same_as_current)
            s.confirmPassword != s.newPassword -> context.getString(R.string.account_password_mismatch)
            else -> null
        }
        if (error != null) {
            _uiState.update { it.copy(errorMessage = error) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }
            try {
                val oldPassword = s.oldPassword.takeIf { s.verifyMode == VerifyMode.OldPassword }
                val code = s.code.takeIf { s.verifyMode == VerifyMode.Code }
                authRepository.changePassword(oldPassword, code, s.newPassword).getOrThrow()
                _uiState.update { it.copy(isSubmitting = false, success = true) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isSubmitting = false, errorMessage = e.message ?: context.getString(R.string.account_change_password_failed))
                }
            }
        }
    }

    private fun startCountdown() {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            for (i in 60 downTo 1) {
                _uiState.update { it.copy(sendCodeCountdown = i) }
                delay(1.seconds)
            }
            _uiState.update { it.copy(sendCodeCountdown = 0) }
        }
    }

    private fun isValidNewPassword(password: String): Boolean {
        if (password.length !in 6..32) return false
        return password.any { it.isLetter() } && password.any { it.isDigit() }
    }
}
