package xyz.larkzhh.lime.data.local

import android.util.Base64
import com.tencent.mmkv.MMKV
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import xyz.larkzhh.lime.domain.ForceLogoutBus
import xyz.larkzhh.lime.domain.LoginRedirectBus
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 本地 Token 存储管理器
 */
@Singleton
class TokenStorage @Inject constructor() {

    private val mmkv by lazy { MMKV.defaultMMKV() }

    /// 解密结果内存缓存
    private var cachedAccess: String? = null
    private var cachedRefresh: String? = null

    /// 当前登录账号 id
    private val _currentUserId = MutableStateFlow(readCurrentUserId())
    val currentUserId: Long? get() = _currentUserId.value

    /// 当前账号变化流
    val currentUserIdFlow: StateFlow<Long?> = _currentUserId.asStateFlow()

    /// 登录状态
    private val _isLoggedIn = MutableStateFlow(mmkv.containsKey(KEY_REFRESH_TOKEN))
    val isLoggedInFlow: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    /// 访问令牌
    var accessToken: String?
        get() = cachedAccess ?: mmkv.decodeString(KEY_ACCESS_TOKEN)
            ?.let { TokenCipher.decrypt(it) }
            ?.also { cachedAccess = it }
        set(value) {
            if (value == null) {
                mmkv.removeValueForKey(KEY_ACCESS_TOKEN)
                cachedAccess = null
            } else {
                TokenCipher.encrypt(value)?.let { encoded ->
                    mmkv.encode(KEY_ACCESS_TOKEN, encoded)
                    cachedAccess = value
                }
            }
        }

    /// 刷新令牌
    var refreshToken: String?
        get() = cachedRefresh ?: mmkv.decodeString(KEY_REFRESH_TOKEN)
            ?.let { TokenCipher.decrypt(it) }
            ?.also { cachedRefresh = it }
        set(value) {
            if (value == null) {
                mmkv.removeValueForKey(KEY_REFRESH_TOKEN)
                cachedRefresh = null
            } else {
                TokenCipher.encrypt(value)?.let { encoded ->
                    mmkv.encode(KEY_REFRESH_TOKEN, encoded)
                    cachedRefresh = value
                }
            }
        }

    /// Token 过期时间戳
    private var expiresAt: Long
        get() = mmkv.decodeLong(KEY_EXPIRES_AT, 0L)
        set(value) { mmkv.encode(KEY_EXPIRES_AT, value) }

    /// 保存用户登录凭证
    fun saveTokens(accessToken: String, refreshToken: String, expiresIn: Long) {
        this.accessToken = accessToken
        this.refreshToken = refreshToken
        this.expiresAt = System.currentTimeMillis() + expiresIn * 1000L// 令牌过期时间
        // 解析账号 id
        val uid = decodeUserId(accessToken)
        _currentUserId.value = uid
        mmkv.encode(KEY_CURRENT_USER_ID, uid ?: -1L)
        _isLoggedIn.value = true
        // 登录、刷新成功
        ForceLogoutBus.clearPending()
        LoginRedirectBus.clear()
    }

    /// 清除所有本地保存的 Token 信息
    fun clearTokens() {
        LoginRedirectBus.mark()
        cachedAccess = null
        cachedRefresh = null
        mmkv.removeValueForKey(KEY_ACCESS_TOKEN)
        mmkv.removeValueForKey(KEY_REFRESH_TOKEN)
        mmkv.removeValueForKey(KEY_EXPIRES_AT)
        _currentUserId.value = null
        mmkv.encode(KEY_CURRENT_USER_ID, -1L)
        _isLoggedIn.value = false
    }

    /// 通过刷新令牌是否存在来判断用户是否处于登录状态（优先走内存缓存，不触发解密）
    fun isLoggedIn(): Boolean = cachedRefresh != null || mmkv.containsKey(KEY_REFRESH_TOKEN)

    /// 判断当前的访问令牌是否有效
    fun isAccessTokenValid(): Boolean =
        !accessToken.isNullOrEmpty() && System.currentTimeMillis() < expiresAt

    private fun readCurrentUserId(): Long? {
        val uid = mmkv.decodeLong(KEY_CURRENT_USER_ID, -1L)
        return uid.takeIf { it > 0 }
    }

    /// 从 JWT 的 payload（sub=userId）解析账号 id
    private fun decodeUserId(accessToken: String): Long? = try {
        val payload = accessToken.split(".").getOrNull(1) ?: return null
        val json = String(Base64.decode(payload, Base64.URL_SAFE or Base64.NO_WRAP))
        JSONObject(json).optString("sub").toLongOrNull()
    } catch (e: Exception) {
        null
    }

    /// 一些和认证相关的常量
    internal companion object {
        const val KEY_ACCESS_TOKEN = "access_token"
        const val KEY_REFRESH_TOKEN = "refresh_token"
        const val KEY_EXPIRES_AT = "expires_at"
        const val KEY_CURRENT_USER_ID = "current_user_id"
    }
}
