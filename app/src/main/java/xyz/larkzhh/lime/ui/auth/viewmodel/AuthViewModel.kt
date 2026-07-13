package xyz.larkzhh.lime.ui.auth.viewmodel

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
import javax.inject.Inject

/// 登录页面 UI 状态
data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val code: String = "",
    val isCodeMode: Boolean = false,
    val passwordVisible: Boolean = false,
    val sendCodeCountdown: Int = 0,
    val isSendingCode: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isSuccess: Boolean = false,
)

/// 注册页面 UI 状态
data class RegisterUiState(
    val email: String = "",
    val password: String = "",
    val code: String = "",
    val phone: String = "",
    val passwordVisible: Boolean = false,
    val sendCodeCountdown: Int = 0,
    val isSendingCode: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isSuccess: Boolean = false,
)

/**
 * 认证 ViewModel
 * 管理登录、注册、登出的 UI 状态与业务逻辑。
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _loginState = MutableStateFlow(LoginUiState())
    val loginState: StateFlow<LoginUiState> = _loginState.asStateFlow()

    private val _registerState = MutableStateFlow(RegisterUiState())
    val registerState: StateFlow<RegisterUiState> = _registerState.asStateFlow()

    private var loginCountdownJob: Job? = null
    private var registerCountdownJob: Job? = null

    // 判断登录状态
    fun isLoggedIn(): Boolean = authRepository.isLoggedIn()

    /**
     * 登录
     */
    /// 邮箱更改
    fun onLoginEmailChange(value: String) =
        _loginState.update { it.copy(email = value, errorMessage = null) }
    /// 密码更改
    fun onLoginPasswordChange(value: String) =
        _loginState.update { it.copy(password = value, errorMessage = null) }
    /// 验证码更改
    fun onLoginCodeChange(value: String) =
        _loginState.update { it.copy(code = value, errorMessage = null) }
    /// 切换密码显示状态
    fun onLoginPasswordVisibilityToggle() =
        _loginState.update { it.copy(passwordVisible = !it.passwordVisible) }
    /// 切换登录方式
    fun onLoginModeChange(isCodeMode: Boolean) =
        _loginState.update { it.copy(isCodeMode = isCodeMode, errorMessage = null) }

    /// 发送验证码
    fun sendLoginCode() {
        val email = _loginState.value.email
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _loginState.update { it.copy(errorMessage = "请输入有效的邮箱地址") }
            return
        }
        viewModelScope.launch {
            _loginState.update { it.copy(isSendingCode = true, errorMessage = null) }
            try {
                authRepository.sendCode(email).getOrThrow()
                startLoginCountdown()
            } catch (e: Exception) {
                _loginState.update { it.copy(errorMessage = e.message ?: "发送失败") }
            } finally {
                _loginState.update { it.copy(isSendingCode = false) }
            }
        }
    }

    /// 倒计时
    private fun startLoginCountdown() {
        loginCountdownJob?.cancel()
        loginCountdownJob = viewModelScope.launch {
            for (i in 60 downTo 1) {
                _loginState.update { it.copy(sendCodeCountdown = i) }
                delay(1000)
            }
            _loginState.update { it.copy(sendCodeCountdown = 0) }
        }
    }

    /// 提交登录
    fun login() {
        val s = _loginState.value
        val validationError = if (s.isCodeMode) validateLoginWithCode(s.email, s.code)
                              else validateLoginWithPassword(s.email, s.password)
        if (validationError != null) {
            _loginState.update { it.copy(errorMessage = validationError) }
            return
        }
        viewModelScope.launch {
            _loginState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                if (s.isCodeMode) {
                    authRepository.login(email = s.email, code = s.code).getOrThrow()
                } else {
                    authRepository.login(email = s.email, password = s.password).getOrThrow()
                }
                _loginState.update { it.copy(isLoading = false, isSuccess = true) }
            } catch (e: Exception) {
                _loginState.update { it.copy(isLoading = false, errorMessage = e.message ?: "登录失败") }
            }
        }
    }

    /// 消费登录成功标志
    fun clearLoginSuccess() = _loginState.update { it.copy(isSuccess = false) }

    /**
     * 注册
     */
    /// 邮箱更改
    fun onRegisterEmailChange(value: String) =
        _registerState.update { it.copy(email = value, errorMessage = null) }
    /// 密码更改
    fun onRegisterPasswordChange(value: String) =
        _registerState.update { it.copy(password = value, errorMessage = null) }
    /// 验证码更改
    fun onRegisterCodeChange(value: String) =
        _registerState.update { it.copy(code = value, errorMessage = null) }
    /// 手机号更改
    fun onRegisterPhoneChange(value: String) =
        _registerState.update { it.copy(phone = value, errorMessage = null) }
    /// 切换密码显示状态
    fun onRegisterPasswordVisibilityToggle() =
        _registerState.update { it.copy(passwordVisible = !it.passwordVisible) }

    /// 发送验证码
    fun sendRegisterCode() {
        val email = _registerState.value.email
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _registerState.update { it.copy(errorMessage = "请输入有效的邮箱地址") }
            return
        }
        viewModelScope.launch {
            _registerState.update { it.copy(isSendingCode = true, errorMessage = null) }
            try {
                authRepository.sendCode(email).getOrThrow()
                startRegisterCountdown()
            } catch (e: Exception) {
                _registerState.update { it.copy(errorMessage = e.message ?: "发送失败") }
            } finally {
                _registerState.update { it.copy(isSendingCode = false) }
            }
        }
    }

    /// 倒计时
    private fun startRegisterCountdown() {
        registerCountdownJob?.cancel()
        registerCountdownJob = viewModelScope.launch {
            for (i in 60 downTo 1) {
                _registerState.update { it.copy(sendCodeCountdown = i) }
                delay(1000)
            }
            _registerState.update { it.copy(sendCodeCountdown = 0) }
        }
    }

    /// 提交注册
    fun register() {
        val s = _registerState.value
        val validationError = validateRegister(s.email, s.password, s.code, s.phone)
        if (validationError != null) {
            _registerState.update { it.copy(errorMessage = validationError) }
            return
        }
        viewModelScope.launch {
            _registerState.update { it.copy(isLoading = true, errorMessage = null) }
            val phone = s.phone.ifEmpty { null }
            try {
                authRepository.register(s.email, s.password, s.code, phone).getOrThrow()
                _registerState.update { it.copy(isLoading = false, isSuccess = true) }
            } catch (e: Exception) {
                _registerState.update { it.copy(isLoading = false, errorMessage = e.message ?: "注册失败") }
            }
        }
    }

    /// 消费注册成功标志
    fun clearRegisterSuccess() = _registerState.update { it.copy(isSuccess = false) }

    /// 登出
    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            _loginState.value = LoginUiState()
            _registerState.value = RegisterUiState()
        }
    }

    /**
     * 校验登录表单（密码模式）
     */
    private fun validateLoginWithPassword(email: String, password: String): String? {
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) return "请输入有效的邮箱地址"
        if (password.isBlank()) return "请输入密码"
        return null
    }

    /**
     * 校验登录表单（验证码模式）
     */
    private fun validateLoginWithCode(email: String, code: String): String? {
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) return "请输入有效的邮箱地址"
        if (code.isBlank()) return "请输入验证码"
        return null
    }

    /**
     * 校验注册表单输入
     * @param email 邮箱地址
     * @param password 密码
     * @param code 邮箱验证码
     * @param phone 手机号
     * @return 失败返回错误提示，通过返回 null
     */
    private fun validateRegister(email: String, password: String, code: String, phone: String): String? {
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) return "请输入有效的邮箱地址"
        if (password.length !in 6..32) return "密码需为 6-32 个字符"
        if (!password.any { it.isLetter() } || !password.any { it.isDigit() }) return "密码必须同时包含字母和数字"
        if (code.isBlank()) return "请输入邮箱验证码"
        if (phone.isNotEmpty() && !Regex("^1\\d{10}$").matches(phone)) return "手机号格式不正确"
        return null
    }
}