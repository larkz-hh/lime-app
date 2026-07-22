package xyz.larkzhh.lime.data.repository

import xyz.larkzhh.lime.data.local.TokenStorage
import xyz.larkzhh.lime.data.network.ApiService
import xyz.larkzhh.lime.data.network.model.LoginRequest
import xyz.larkzhh.lime.data.network.model.RefreshTokenRequest
import xyz.larkzhh.lime.data.network.model.RegisterRequest
import xyz.larkzhh.lime.data.network.model.SendCodeRequest
import xyz.larkzhh.lime.data.network.model.TokenData
import xyz.larkzhh.lime.domain.repository.AuthRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 认证数据仓库实现类
 * 协调网络请求和本地存储
 * 实现 AuthRepository 接口定义的方法
 */
@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
    private val tokenStorage: TokenStorage,
) : AuthRepository {

    /**
     * 发送邮箱验证码
     * @param email 目标邮箱
     * @throws Exception 发送失败时抛出
     */
    override suspend fun sendCode(email: String): Result<Unit> = runCatching {
        val response = apiService.sendCode(SendCodeRequest(email))
        check(response.code == 200) { response.message }
    }

    /**
     * 用户登录
     * @param email 登录邮箱
     * @param password 登录密码（与 code 二选一）
     * @param code 邮箱验证码（与 password 二选一）
     * @return 包含登录凭证的结果对象
     * @throws Exception 登录失败时抛出
     */
    override suspend fun login(email: String, password: String?, code: String?): Result<TokenData> = runCatching {
        val response = apiService.login(LoginRequest(email = email, password = password, code = code))
        check(response.code == 200 && response.data != null) { response.message }
        tokenStorage.saveTokens(response.data.accessToken, response.data.refreshToken, response.data.expiresIn)
        response.data
    }

    /**
     * 用户注册
     * @param email 邮箱
     * @param password 密码
     * @param code 邮箱验证码
     * @param phone 手机号（可选）
     * @return 注册结果
     * @throws Exception 注册失败时抛出
     */
    override suspend fun register(
        email: String,
        password: String,
        code: String,
        phone: String?,
    ): Result<Unit> = runCatching {
        val response = apiService.register(RegisterRequest(email = email, password = password, code = code, phone = phone))
        check(response.code == 200) { response.message }
    }

    /**
     * 刷新访问令牌
     * @return 刷新结果
     */
    override suspend fun refreshToken(): Result<TokenData> = runCatching {
        val refreshToken = tokenStorage.refreshToken ?: error("未登录")
        val response = apiService.refreshToken(RefreshTokenRequest(refreshToken))
        if (response.code != 200 || response.data == null) {
            tokenStorage.clearTokens()
            error(response.message)  // 刷新失败需要先清除本地 Token 再抛出
        }
        // 刷新成功后，用新的 Token 覆盖本地旧凭证
        tokenStorage.saveTokens(response.data.accessToken, response.data.refreshToken, response.data.expiresIn)
        response.data // 返回新的 Token 数据
    }

    /**
     * 登出
     */
    override suspend fun logout() {
        runCatching { apiService.logout() }
        tokenStorage.clearTokens()
    }

    /**
     * 登陆状态判断
     * @return 是否已登录
     */
    override fun isLoggedIn(): Boolean = tokenStorage.isLoggedIn()
}
