package xyz.larkzhh.lime.ui.theme

import com.tencent.mmkv.MMKV

/**
 * 启动动画开关
 */
object AppSplashAnim {

    private const val KEY_NAME = "splash_animation"

    private val mmkv: MMKV by lazy { MMKV.defaultMMKV() }

    fun enabled(): Boolean = mmkv.decodeBool(KEY_NAME, true)

    fun setEnabled(value: Boolean) {
        mmkv.encode(KEY_NAME, value)
    }
}
