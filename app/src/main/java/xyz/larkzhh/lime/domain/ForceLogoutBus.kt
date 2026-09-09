package xyz.larkzhh.lime.domain

import com.tencent.mmkv.MMKV
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * 强制下线事件
 *
 * 触发 SSE 收到 reason=login_elsewhere 的 kick、IM KickedOffline、refresh 凭证失效。
 */
object ForceLogoutBus {

    private const val KEY_PENDING_FORCE_LOGOUT = "force_logout_pending"

    private val _events = MutableSharedFlow<Unit>(extraBufferCapacity = 4)
    val events: SharedFlow<Unit> = _events.asSharedFlow()

    fun emit() {
        MMKV.defaultMMKV().encode(KEY_PENDING_FORCE_LOGOUT, true)
        _events.tryEmit(Unit)
    }

    /// 消费待处理下线标志
    fun consumePending(): Boolean {
        val pending = MMKV.defaultMMKV().decodeBool(KEY_PENDING_FORCE_LOGOUT, false)
        if (pending) {
            clearPending()
        }
        return pending
    }

    /// 直接清除待处理下线标志
    fun clearPending() {
        MMKV.defaultMMKV().encode(KEY_PENDING_FORCE_LOGOUT, false)
    }
}
