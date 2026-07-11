package xyz.larkzhh.lime.data.repository

import xyz.larkzhh.lime.data.local.TokenStorage
import xyz.larkzhh.lime.data.network.ApiService
import xyz.larkzhh.lime.data.network.model.LoginRequest
import xyz.larkzhh.lime.data.network.model.RefreshTokenRequest
import xyz.larkzhh.lime.data.network.model.RegisterRequest
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
     * 用户登录
     * @param username 用户名
     * @param password 密码
     * @return 包含登录凭证的结果对象
     * @throws Exception 登录失败时抛出
     */
    override suspend fun login(username: String, password: String): Result<TokenData> = runCatching {
        val response = apiService.login(LoginRequest(username, password))
        if (response.code == 200 && response.data != null) {
            tokenStorage.saveTokens(response.data.accessToken, response.data.refreshToken, response.data.expiresIn)
            response.data
        } else {
            throw Exception(response.message)
        }
    }

    /**
     * 用户注册
     * @param username 用户名
     * @param password 密码
     * @param email 邮箱
     * @param phone 手机号（可选）
     * @return 注册结果
     * @throws Exception 注册失败时抛出
     */
    override suspend fun register(
        username: String,
        password: String,
        email: String,
        phone: String?,
    ): Result<Unit> = runCatching {
        val response = apiService.register(RegisterRequest(username, password, email, phone))
        if (response.code != 200) throw Exception(response.message)
    }

    /**
     * 刷新访问令牌
     * @return 刷新结果
     */
    override suspend fun refreshToken(): Result<TokenData> = runCatching {
        val refreshToken = tokenStorage.refreshToken ?: throw Exception("未登录")
        val response = apiService.refreshToken(RefreshTokenRequest(refreshToken))
        if (response.code == 200 && response.data != null) {
            // 刷新成功后，用新的 Token 覆盖本地旧凭证
            tokenStorage.saveTokens(response.data.accessToken, response.data.refreshToken, response.data.expiresIn)
            response.data// 返回新的 Token 数据
        } else {
            tokenStorage.clearTokens()
            throw Exception(response.message)
        }
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
