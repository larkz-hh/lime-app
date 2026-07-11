package xyz.larkzhh.lime.ui.auth.viewmodel

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import xyz.larkzhh.lime.domain.repository.AuthRepository
import javax.inject.Inject

/// 登录页面 UI 状态
data class LoginUiState(
    val username: String = "",
    val password: String = "",
    val passwordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isSuccess: Boolean = false,
)

/// 注册页面 UI 状态
data class RegisterUiState(
    val username: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val phone: String = "",
    val passwordVisible: Boolean = false,
    val confirmPasswordVisible: Boolean = false,
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

    // 判断登录状态
    fun isLoggedIn(): Boolean = authRepository.isLoggedIn()

    /**
     * 登录
     */
    /// 用户名更改
    fun onLoginUsernameChange(value: String) =
        _loginState.update { it.copy(username = value, errorMessage = null) }// 清除错误提示
    /// 密码更改
    fun onLoginPasswordChange(value: String) =
        _loginState.update { it.copy(password = value, errorMessage = null) }
    /// 切换密码显示状态
    fun onLoginPasswordVisibilityToggle() =
        _loginState.update { it.copy(passwordVisible = !it.passwordVisible) }

    /// 提交登录
    fun login() {
        val username = _loginState.value.username
        val password = _loginState.value.password
        val validationError = validateLogin(username, password)
        if (validationError != null) {
            _loginState.update { it.copy(errorMessage = validationError) }
            return
        }
        viewModelScope.launch {
            _loginState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                authRepository.login(username, password).getOrThrow()
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
    /// 用户名更改
    fun onRegisterUsernameChange(value: String) =
        _registerState.update { it.copy(username = value, errorMessage = null) }
    /// 邮箱更改
    fun onRegisterEmailChange(value: String) =
        _registerState.update { it.copy(email = value, errorMessage = null) }
    /// 密码更改
    fun onRegisterPasswordChange(value: String) =
        _registerState.update { it.copy(password = value, errorMessage = null) }
    /// 再次确认密码更改
    fun onRegisterConfirmPasswordChange(value: String) =
        _registerState.update { it.copy(confirmPassword = value, errorMessage = null) }
    /// 手机号更改
    fun onRegisterPhoneChange(value: String) =
        _registerState.update { it.copy(phone = value, errorMessage = null) }

    /// 密码显示更改
    fun onRegisterPasswordVisibilityToggle() =
        _registerState.update { it.copy(passwordVisible = !it.passwordVisible) }
    fun onRegisterConfirmPasswordVisibilityToggle() =
        _registerState.update { it.copy(confirmPasswordVisible = !it.confirmPasswordVisible) }

    /// 提交注册
    fun register() {
        val s = _registerState.value
        val validationError = validateRegister(s.username, s.email, s.password, s.confirmPassword, s.phone)
        if (validationError != null) {
            _registerState.update { it.copy(errorMessage = validationError) }
            return
        }
        viewModelScope.launch {
            _registerState.update { it.copy(isLoading = true, errorMessage = null) }
            val phone = _registerState.value.phone.ifEmpty { null }
            try {
                authRepository.register(s.username, s.password, s.email, phone).getOrThrow()
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
            _loginState.value = LoginUiState()// 重置为初始值
            _registerState.value = RegisterUiState()
        }
    }

    /**
     * 校验登录表单输入
     * @param username 用户名
     * @param password 密码
     * @return 失败返回错误提示，通过返回 null
     */
    private fun validateLogin(username: String, password: String): String? {
        if (username.isBlank()) return "请输入用户名"
        if (password.isBlank()) return "请输入密码"
        return null
    }

    /**
     * 校验注册表单输入
     * @param username 用户名
     * @param email 邮箱地址
     * @param password 密码
     * @param confirmPassword 确认密码
     * @param phone 手机号
     * @return 失败返回错误提示，通过返回 null
     */
    private fun validateRegister(
        username: String,
        email: String,
        password: String,
        confirmPassword: String,
        phone: String,
    ): String? {
        if (username.length !in 3..20) return "用户名需为 3-20 个字符"
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) return "请输入有效的邮箱地址"
        if (password.length !in 6..32) return "密码需为 6-32 个字符"
        if (!password.any { it.isLetter() } || !password.any { it.isDigit() }) return "密码必须同时包含字母和数字"
        if (password != confirmPassword) return "两次输入的密码不一致"
        if (phone.isNotEmpty() && !Regex("^1\\d{10}$").matches(phone)) return "手机号格式不正确"
        return null
    }
}