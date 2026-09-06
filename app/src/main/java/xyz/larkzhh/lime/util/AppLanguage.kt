package xyz.larkzhh.lime.util

import android.content.Context
import android.content.res.Configuration
import com.tencent.mmkv.MMKV
import java.util.Locale

/**
 * 全局应用语言
 */
object AppLanguage {

    private const val KEY = "app_language"
    const val TAG_SYSTEM = "system"
    const val TAG_SIMPLIFIED = "zh-CN"
    const val TAG_TRADITIONAL = "zh-TW"
    const val TAG_ENGLISH = "en"

    private val mmkv: MMKV by lazy { MMKV.defaultMMKV() }

    fun currentTag(): String = mmkv.decodeString(KEY, TAG_SYSTEM) ?: TAG_SYSTEM

    fun setTag(tag: String) {
        mmkv.encode(KEY, tag)
    }

    fun wrap(context: Context): Context {
        val tag = currentTag()
        if (tag == TAG_SYSTEM) return context
        val locale = Locale.forLanguageTag(tag)
        val configuration = Configuration(context.resources.configuration)
        configuration.setLocale(locale)
        return context.createConfigurationContext(configuration)
    }
}
