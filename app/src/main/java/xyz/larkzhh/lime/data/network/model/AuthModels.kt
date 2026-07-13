package xyz.larkzhh.lime.data.network.model

/**
 * 认证请求响应模型
 */

/// 响应
data class ApiResponse<T>(
    val code: Int,
    val message: String,
    val data: T?,
)

/// 令牌
data class TokenData(
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Long,
)

/// 发送验证码
data class SendCodeRequest(
    val email: String,
)

/// 登录（password 与 code 二选一）
data class LoginRequest(
    val email: String,
    val password: String? = null,
    val code: String? = null,
)

/// 注册
data class RegisterRequest(
    val email: String,
    val password: String,
    val code: String,
    val phone: String? = null,
)

/// 刷新令牌
data class RefreshTokenRequest(
    val refreshToken: String,
)
