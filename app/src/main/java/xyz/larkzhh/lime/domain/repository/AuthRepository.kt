package xyz.larkzhh.lime.domain.repository

import xyz.larkzhh.lime.data.network.model.TokenData

/**
 * 认证数据仓库接口
 */
interface AuthRepository {
    /**
     * 用户登录
     * @param username 用户名
     * @param password 密码
     * @return 包含登录凭证的结果对象
     */
    suspend fun login(username: String, password: String): Result<TokenData>
    /**
     * 用户注册
     * @param username 用户名
     * @param password 密码
     * @param email 邮箱
     * @param phone 手机号（可选）
     * @return 注册结果
     */
    suspend fun register(username: String, password: String, email: String, phone: String?): Result<Unit>
    /**
     * 刷新访问令牌
     * @return 刷新结果
     */
    suspend fun refreshToken(): Result<TokenData>
    /// 用户登出
    suspend fun logout()

    /// 检查登录状态
    fun isLoggedIn(): Boolean
}
