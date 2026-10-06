package xyz.larkzhh.lime.ui.theme

import com.tencent.mmkv.MMKV
import xyz.larkzhh.lime.data.local.TokenStorage
import xyz.larkzhh.lime.data.local.UserPreferences

/**
 * 字体偏好的本地读写（按账号分片）
 */
object AppFont {

    private const val KEY_NAME = UserPreferences.Keys.FONT

    private val mmkv: MMKV by lazy { MMKV.defaultMMKV() }

    private fun storageKey(): String {
        val uid = mmkv.decodeLong(TokenStorage.KEY_CURRENT_USER_ID, -1L)
        return if (uid > 0) "pref_${uid}_$KEY_NAME" else "pref_$KEY_NAME"
    }

    fun currentTag(): String = mmkv.decodeString(storageKey(), FontOption.SYSTEM.tag) ?: FontOption.SYSTEM.tag

    fun setTag(tag: String) {
        mmkv.encode(storageKey(), tag)
    }
}
