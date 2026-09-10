package xyz.larkzhh.lime.data.local

import com.tencent.mmkv.MMKV
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/// 翻译方式
enum class TranslateMode(val label: String) {
    Auto("自动"),
    Offline("仅离线（本地语言包）"),
}

/**
 * 翻译方式设置
 */
@Singleton
class TranslateSettings @Inject constructor(
    private val mmkv: MMKV,
) {

    private val _mode = MutableStateFlow(load())
    val mode: StateFlow<TranslateMode> = _mode.asStateFlow()

    fun setMode(mode: TranslateMode) {
        mmkv.encode(KEY_MODE, mode.name)
        _mode.value = mode
    }

    private fun load(): TranslateMode =
        mmkv.decodeString(KEY_MODE)
            ?.let { name -> TranslateMode.entries.firstOrNull { it.name == name } }
            ?: TranslateMode.Auto

    private companion object {
        const val KEY_MODE = "translate_mode"
    }
}
