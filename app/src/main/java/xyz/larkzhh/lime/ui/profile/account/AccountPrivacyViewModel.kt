package xyz.larkzhh.lime.ui.profile.account

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import xyz.larkzhh.lime.domain.repository.AuthRepository
import xyz.larkzhh.lime.domain.repository.UserRepository
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

/// 身份验证方式
enum class VerifyMode(val label: String) {
    OldPassword("原密码验证"),
    Code("邮箱验证码"),
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
                        privacyError = e.message ?: "设置失败，请重试",
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
            _uiState.update { it.copy(errorMessage = "无法获取账号邮箱，请改用原密码验证") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSendingCode = true, errorMessage = null) }
            try {
                authRepository.sendCode(email).getOrThrow()
                startCountdown()
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message ?: "验证码发送失败") }
            } finally {
                _uiState.update { it.copy(isSendingCode = false) }
            }
        }
    }

    /// 提交修改密码
    fun submitChangePassword() {
        val s = _uiState.value
        val error = when {
            s.verifyMode == VerifyMode.OldPassword && s.oldPassword.isBlank() -> "请输入当前密码"
            s.verifyMode == VerifyMode.Code && s.code.isBlank() -> "请输入邮箱验证码"
            !isValidNewPassword(s.newPassword) -> "新密码需为 6-32 位，且同时包含字母和数字"
            s.verifyMode == VerifyMode.OldPassword && s.newPassword == s.oldPassword -> "新密码不能与当前密码相同"
            s.confirmPassword != s.newPassword -> "两次输入的新密码不一致"
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
                // 清空本地用户缓存，重新登录
                authRepository.changePassword(oldPassword, code, s.newPassword).getOrThrow()
                userRepository.clearUser()
                _uiState.update { it.copy(isSubmitting = false, success = true) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isSubmitting = false, errorMessage = e.message ?: "修改失败，请重试")
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
