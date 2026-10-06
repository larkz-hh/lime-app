package xyz.larkzhh.lime.data.repository

import xyz.larkzhh.lime.data.local.TokenStorage
import xyz.larkzhh.lime.data.network.auth.AuthApi
import xyz.larkzhh.lime.data.network.model.ChangePasswordRequest
import xyz.larkzhh.lime.data.network.model.LoginRequest
import xyz.larkzhh.lime.data.network.model.RefreshTokenRequest
import xyz.larkzhh.lime.data.network.model.RegisterRequest
import xyz.larkzhh.lime.data.network.model.SendCodeRequest
import xyz.larkzhh.lime.data.network.model.TokenData
import xyz.larkzhh.lime.domain.repository.AuthRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 认证数据仓库实现
 */
@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val apiService: AuthApi,
    private val tokenStorage: TokenStorage,
) : AuthRepository {

    /// 发送邮箱验证码
    override suspend fun sendCode(email: String): Result<Unit> = runCatching {
        val response = apiService.sendCode(SendCodeRequest(email))
        check(response.code == 200) { response.message }
    }

    /// 用户登录
    override suspend fun login(email: String, password: String?, code: String?): Result<TokenData> = runCatching {
        val response = apiService.login(LoginRequest(email = email, password = password, code = code))
        val data = response.data
        check(response.code == 200 && data != null) { response.message }
        tokenStorage.saveTokens(data.accessToken, data.refreshToken, data.expiresIn)
        data
    }

    /// 用户注册
    override suspend fun register(
        email: String,
        password: String,
        code: String,
        phone: String?,
    ): Result<Unit> = runCatching {
        val response = apiService.register(RegisterRequest(email = email, password = password, code = code, phone = phone))
        check(response.code == 200) { response.message }
    }

    /// 刷新访问令牌
    override suspend fun refreshToken(): Result<TokenData> = runCatching {
        val refreshToken = tokenStorage.refreshToken ?: error("未登录")
        val response = apiService.refreshToken(RefreshTokenRequest(refreshToken))
        val data = response.data
        if (response.code != 200 || data == null) {
            tokenStorage.clearTokens()
            error(response.message)  // 刷新失败需要先清除本地 Token 再抛出
        }
        // 刷新成功后，用新的 Token 覆盖本地旧凭证
        tokenStorage.saveTokens(data.accessToken, data.refreshToken, data.expiresIn)
        data // 返回新的 Token 数据
    }

    /**
     * 登出
     */
    override suspend fun logout() {
        runCatching { apiService.logout() }
        tokenStorage.clearTokens()
    }

    /// 修改密码
    override suspend fun changePassword(
        oldPassword: String?,
        code: String?,
        newPassword: String,
    ): Result<Unit> = runCatching {
        val response = apiService.changePassword(
            ChangePasswordRequest(oldPassword = oldPassword, code = code, newPassword = newPassword),
        )
        check(response.code == 200) { response.message }
        val data = response.data
        if (data != null) {
            // 改密成功返回新的双 token
            tokenStorage.saveTokens(data.accessToken, data.refreshToken, data.expiresIn)
        } else {
            tokenStorage.clearTokens()
        }
    }

    /// 登陆状态判断
    override fun isLoggedIn(): Boolean = tokenStorage.isLoggedIn()
}
