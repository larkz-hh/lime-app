package xyz.larkzhh.lime.data.local

import android.util.Base64
import com.tencent.mmkv.MMKV
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import xyz.larkzhh.lime.domain.ForceLogoutBus
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 本地 Token 存储管理器
 */
@Singleton
class TokenStorage @Inject constructor() {

    private val mmkv by lazy { MMKV.defaultMMKV() }

    /// 当前登录账号 id（仅当 refresh token 能正常解出时才认为有登录态，防止升级后残留旧 uid）
    private val _currentUserId = MutableStateFlow(
        if (refreshToken.isNullOrEmpty()) null else readCurrentUserId()
    )
    val currentUserId: Long? get() = _currentUserId.value

    /// 当前账号变化流
    val currentUserIdFlow: StateFlow<Long?> = _currentUserId.asStateFlow()

    /// 登录状态
    private val _isLoggedIn = MutableStateFlow(!refreshToken.isNullOrEmpty())
    val isLoggedInFlow: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    /// 访问令牌
    var accessToken: String?
        get() = mmkv.decodeString(KEY_ACCESS_TOKEN)?.let { TokenCipher.decrypt(it) }
        set(value) {
            if (value == null) {
                mmkv.removeValueForKey(KEY_ACCESS_TOKEN)
            } else {
                TokenCipher.encrypt(value)?.let { mmkv.encode(KEY_ACCESS_TOKEN, it) }
            }
        }

    /// 刷新令牌
    var refreshToken: String?
        get() = mmkv.decodeString(KEY_REFRESH_TOKEN)?.let { TokenCipher.decrypt(it) }
        set(value) {
            if (value == null) {
                mmkv.removeValueForKey(KEY_REFRESH_TOKEN)
            } else {
                TokenCipher.encrypt(value)?.let { mmkv.encode(KEY_REFRESH_TOKEN, it) }
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
    }

    /// 清除所有本地保存的 Token 信息
    fun clearTokens() {
        mmkv.removeValueForKey(KEY_ACCESS_TOKEN)
        mmkv.removeValueForKey(KEY_REFRESH_TOKEN)
        mmkv.removeValueForKey(KEY_EXPIRES_AT)
        _currentUserId.value = null
        mmkv.encode(KEY_CURRENT_USER_ID, -1L)
        _isLoggedIn.value = false
    }

    /// 通过刷新令牌是否存在来判断用户是否处于登录状态
    fun isLoggedIn(): Boolean = !refreshToken.isNullOrEmpty()

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
