package xyz.larkzhh.lime.util.cache

import com.google.gson.Gson
import com.tencent.mmkv.MMKV
import java.lang.reflect.Type
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 列表 JSON 缓存
 */
@Singleton
class JsonListCache @Inject constructor(
    private val gson: Gson,
    private val mmkv: MMKV,
) {

    fun <T> save(key: String, list: List<T>) {
        mmkv.encode(key, gson.toJson(list))
    }

    fun <T> read(key: String, type: Type): List<T>? = runCatching {
        mmkv.decodeString(key)?.let { json ->
            gson.fromJson<List<T>>(json, type)
        }
    }.getOrNull()
}
