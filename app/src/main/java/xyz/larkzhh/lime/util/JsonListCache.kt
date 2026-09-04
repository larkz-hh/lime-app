package xyz.larkzhh.lime.util

import com.google.gson.Gson
import com.tencent.mmkv.MMKV
import java.lang.reflect.Type

/**
 * 列表 JSON 缓存。
 */
object JsonListCache {
    private val gson = Gson()
    private val mmkv: MMKV by lazy { MMKV.defaultMMKV() }

    fun <T> save(key: String, list: List<T>) {
        mmkv.encode(key, gson.toJson(list))
    }

    fun <T> read(key: String, type: Type): List<T>? = runCatching {
        mmkv.decodeString(key)?.let { json ->
            gson.fromJson<List<T>>(json, type)
        }
    }.getOrNull()
}
