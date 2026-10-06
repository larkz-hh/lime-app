package xyz.larkzhh.lime.util.cache

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/// 有界 LRU 缓存
class LruCacheTest {

    @Test
    fun 命中返回缓存值() {
        val cache = LruCache<String, Int>(maxSize = 3)
        cache["a"] = 1
        assertEquals(1, cache["a"])
    }

    @Test
    fun 未命中返回null() {
        val cache = LruCache<String, Int>(maxSize = 3)
        assertNull(cache["missing"])
    }

    @Test
    fun 同键覆盖() {
        val cache = LruCache<String, Int>(maxSize = 3)
        cache["a"] = 1
        cache["a"] = 2
        assertEquals(2, cache["a"])
    }

    @Test
    fun getOrPut命中时不重新计算() {
        val cache = LruCache<String, Int>(maxSize = 3)
        var calls = 0

        val first = cache.getOrPut("k") { calls++; 42 }
        val second = cache.getOrPut("k") { calls++; 99 }

        assertEquals(42, first)
        assertEquals(42, second)
        assertEquals(1, calls)
    }

    @Test
    fun getOrPut未命中时写入缓存() {
        val cache = LruCache<String, Int>(maxSize = 3)
        cache.getOrPut("k") { 7 }
        assertEquals(7, cache["k"])
    }

    @Test
    fun 超出容量淘汰最旧条目并回调() {
        val evicted = mutableListOf<String>()
        val cache = object : LruCache<String, Int>(maxSize = 2) {
            override fun entryEvicted(key: String, value: Int) {
                evicted += key
            }
        }

        cache["a"] = 1
        cache["b"] = 2
        cache["c"] = 3

        assertEquals(listOf("a"), evicted)
        assertNull(cache["a"])
        assertEquals(2, cache["b"])
        assertEquals(3, cache["c"])
    }

    @Test
    fun 读取过的条目不先被淘汰() {
        val evicted = mutableListOf<String>()
        val cache = object : LruCache<String, Int>(maxSize = 2) {
            override fun entryEvicted(key: String, value: Int) {
                evicted += key
            }
        }

        cache["a"] = 1
        cache["b"] = 2
        cache["a"]

        cache["c"] = 3

        assertEquals(listOf("b"), evicted)
        assertEquals(1, cache["a"])
        assertNull(cache["b"])
    }

    @Test
    fun 主动覆盖不触发淘汰回调() {
        val evicted = mutableListOf<String>()
        val cache = object : LruCache<String, Int>(maxSize = 2) {
            override fun entryEvicted(key: String, value: Int) {
                evicted += key
            }
        }

        cache["a"] = 1
        cache["a"] = 2

        assertEquals(emptyList<String>(), evicted)
    }
}
