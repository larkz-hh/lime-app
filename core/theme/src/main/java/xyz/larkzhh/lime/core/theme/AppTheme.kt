package xyz.larkzhh.lime.core.theme

import com.tencent.mmkv.MMKV
import xyz.larkzhh.lime.data.local.TokenStorage
import xyz.larkzhh.lime.ui.theme.ThemeOption
import xyz.larkzhh.lime.data.local.UserPreferences

/**
 * 主题偏好的本地读写（按账号分片）
 */
object AppTheme {

    private const val KEY_NAME = UserPreferences.Keys.THEME

    private val mmkv: MMKV by lazy { MMKV.defaultMMKV() }

    private fun storageKey(): String {
        val uid = mmkv.decodeLong(TokenStorage.KEY_CURRENT_USER_ID, -1L)
        return if (uid > 0) "pref_${uid}_$KEY_NAME" else "pref_$KEY_NAME"
    }

    fun currentTag(): String = mmkv.decodeString(storageKey(), ThemeOption.GREEN.tag) ?: ThemeOption.GREEN.tag

    fun setTag(tag: String) {
        mmkv.encode(storageKey(), tag)
    }
}
