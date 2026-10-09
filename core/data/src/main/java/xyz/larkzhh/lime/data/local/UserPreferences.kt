package xyz.larkzhh.lime.data.local

import com.tencent.mmkv.MMKV
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 用户偏好存储
 */
@Singleton
class UserPreferences @Inject constructor(
    private val tokenStorage: TokenStorage,
    private val mmkv: MMKV,
) {

    private fun key(name: String): String {
        val uid = tokenStorage.currentUserId
        return if (uid != null) "pref_${uid}_$name" else "pref_$name"
    }

    fun getBoolean(name: String, default: Boolean = false): Boolean =
        mmkv.decodeBool(key(name), default)

    fun setBoolean(name: String, value: Boolean) {
        mmkv.encode(key(name), value)
    }

    fun getInt(name: String, default: Int = 0): Int =
        mmkv.decodeInt(key(name), default)

    fun setInt(name: String, value: Int) {
        mmkv.encode(key(name), value)
    }

    fun getLong(name: String, default: Long = 0L): Long =
        mmkv.decodeLong(key(name), default)

    fun setLong(name: String, value: Long) {
        mmkv.encode(key(name), value)
    }

    fun getString(name: String, default: String? = null): String? =
        mmkv.decodeString(key(name), default)

    fun setString(name: String, value: String?) {
        if (value == null) mmkv.removeValueForKey(key(name))
        else mmkv.encode(key(name), value)
    }

    /// 删除某项
    fun remove(name: String) {
        mmkv.removeValueForKey(key(name))
    }

    /// 常用偏好键
    object Keys {
        const val NOTIFY_ENABLED = "notify_enabled"// 通知
        const val THEME = "theme"// 主题
        const val FONT = "font"// 字体
    }
}
