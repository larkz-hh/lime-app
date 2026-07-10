package xyz.larkzhh.lime.data.local

import com.tencent.mmkv.MMKV
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 本地 Token 存储管理器
 */
@Singleton
class TokenStorage @Inject constructor() {

    private val mmkv by lazy { MMKV.defaultMMKV() }

    /// 访问令牌
    var accessToken: String?
        get() = mmkv.decodeString(KEY_ACCESS_TOKEN)
        set(value) = if (value != null) mmkv.encode(KEY_ACCESS_TOKEN, value).let {} else mmkv.removeValueForKey(KEY_ACCESS_TOKEN)

    /// 刷新令牌
    var refreshToken: String?
        get() = mmkv.decodeString(KEY_REFRESH_TOKEN)
        set(value) = if (value != null) mmkv.encode(KEY_REFRESH_TOKEN, value).let {} else mmkv.removeValueForKey(KEY_REFRESH_TOKEN)

    /// Token 过期时间戳
    private var expiresAt: Long
        get() = mmkv.decodeLong(KEY_EXPIRES_AT, 0L)
        set(value) { mmkv.encode(KEY_EXPIRES_AT, value) }

    /**
     * 保存用户登录凭证
     * @param accessToken 访问令牌
     * @param refreshToken 刷新令牌
     * @param expiresIn 令牌的有效时长（单位：秒）
     */
    fun saveTokens(accessToken: String, refreshToken: String, expiresIn: Long) {
        this.accessToken = accessToken
        this.refreshToken = refreshToken
        this.expiresAt = System.currentTimeMillis() + expiresIn * 1000L// 令牌过期时间
    }

    /**
     * 清除所有本地保存的 Token 信息
     */
    fun clearTokens() {
        mmkv.removeValueForKey(KEY_ACCESS_TOKEN)
        mmkv.removeValueForKey(KEY_REFRESH_TOKEN)
        mmkv.removeValueForKey(KEY_EXPIRES_AT)
    }

    /**
     * 通过刷新令牌是否存在来判断用户是否处于登录状态
     */
    fun isLoggedIn(): Boolean = !refreshToken.isNullOrEmpty()

    /**
     * 判断当前的访问令牌是否有效
     */
    fun isAccessTokenValid(): Boolean =
        !accessToken.isNullOrEmpty() && System.currentTimeMillis() < expiresAt

    /// 一些和认证相关的常量
    private companion object {
        const val KEY_ACCESS_TOKEN = "access_token"
        const val KEY_REFRESH_TOKEN = "refresh_token"
        const val KEY_EXPIRES_AT = "expires_at"
    }
}
