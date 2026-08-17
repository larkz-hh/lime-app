package xyz.larkzhh.lime.util

/// 有界最近最少使用缓存，超过容量时自动淘汰最久未访问的条目
class LruCache<K, V>(private val maxSize: Int) {
    private val map = object : LinkedHashMap<K, V>(maxSize, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<K, V>?): Boolean = size > maxSize
    }

    @Synchronized
    operator fun get(key: K): V? = map[key]

    @Synchronized
    operator fun set(key: K, value: V) {
        map[key] = value
    }

    @Synchronized
    fun getOrPut(key: K, defaultValue: () -> V): V = map.getOrPut(key, defaultValue)
}
