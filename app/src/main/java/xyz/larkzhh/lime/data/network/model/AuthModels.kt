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

/// 登录
data class LoginRequest(
    val username: String,
    val password: String,
)

/// 注册
data class RegisterRequest(
    val username: String,
    val password: String,
    val email: String,
    val phone: String? = null,
)

/// 刷新令牌
data class RefreshTokenRequest(
    val refreshToken: String,
)
