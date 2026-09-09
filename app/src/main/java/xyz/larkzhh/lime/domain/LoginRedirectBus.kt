package xyz.larkzhh.lime.domain

import com.tencent.mmkv.MMKV

/**
 * 强制回登录页标记
 */
object LoginRedirectBus {

    private const val KEY_FORCE_LOGIN_PENDING = "force_login_pending"

    /// 标记
    fun mark() {
        MMKV.defaultMMKV().encode(KEY_FORCE_LOGIN_PENDING, true)
    }

    /// 消费并清除标记
    fun consume(): Boolean {
        val pending = MMKV.defaultMMKV().decodeBool(KEY_FORCE_LOGIN_PENDING, false)
        if (pending) {
            clear()
        }
        return pending
    }

    /// 直接清除标记
    fun clear() {
        MMKV.defaultMMKV().encode(KEY_FORCE_LOGIN_PENDING, false)
    }
}
