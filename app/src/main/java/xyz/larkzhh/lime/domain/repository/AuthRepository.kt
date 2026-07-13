package xyz.larkzhh.lime.domain.repository

import xyz.larkzhh.lime.data.network.model.TokenData

/**
 * 认证数据仓库接口
 */
interface AuthRepository {
    /**
     * 发送邮箱验证码
     * @param email 目标邮箱
     * @return 发送结果
     */
    suspend fun sendCode(email: String): Result<Unit>

    /**
     * 用户登录（密码登录传 password，验证码登录传 code，二选一）
     * @param email 登录邮箱
     * @param password 登录密码
     * @param code 邮箱验证码
     * @return 包含登录凭证的结果对象
     */
    suspend fun login(email: String, password: String? = null, code: String? = null): Result<TokenData>
    /**
     * 用户注册
     * @param email 邮箱，作为登录账号
     * @param password 密码
     * @param code 邮箱验证码
     * @param phone 手机号（可选）
     * @return 注册结果
     */
    suspend fun register(email: String, password: String, code: String, phone: String?): Result<Unit>
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
