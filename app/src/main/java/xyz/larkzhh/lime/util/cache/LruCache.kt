package xyz.larkzhh.lime.util.cache

import androidx.collection.LruCache as AndroidXLruCache

/**
 * 有界最近最少使用缓存
 */
open class LruCache<K : Any, V : Any>(maxSize: Int) : AndroidXLruCache<K, V>(maxSize) {

    operator fun set(key: K, value: V) {
        put(key, value)
    }

    /// 命中返回缓存值
    fun getOrPut(key: K, defaultValue: () -> V): V =
        this[key] ?: defaultValue().also { put(key, it) }

    /// 条目因容量淘汰时回调
    protected open fun entryEvicted(key: K, value: V) = Unit

    final override fun entryRemoved(evicted: Boolean, key: K, oldValue: V, newValue: V?) {
        if (evicted) entryEvicted(key, oldValue)
    }
}
