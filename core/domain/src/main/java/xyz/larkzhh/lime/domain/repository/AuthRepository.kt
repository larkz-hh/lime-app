package xyz.larkzhh.lime.domain.repository

import xyz.larkzhh.lime.data.network.model.TokenData

/**
 * 认证数据仓库接口
 */
interface AuthRepository {
    /// 发送邮箱验证码
    suspend fun sendCode(email: String): Result<Unit>

    /// 用户登录
    suspend fun login(email: String, password: String? = null, code: String? = null): Result<TokenData>
    /// 用户注册
    suspend fun register(email: String, password: String, code: String, phone: String?): Result<Unit>
    /// 刷新访问令牌
    suspend fun refreshToken(): Result<TokenData>
    /// 用户登出
    suspend fun logout()

    /// 修改密码
    suspend fun changePassword(oldPassword: String?, code: String?, newPassword: String): Result<Unit>

    /// 检查登录状态
    fun isLoggedIn(): Boolean
}
