package xyz.larkzhh.lime.data.local

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.tencent.mmkv.MMKV
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 搜索历史本地存储
 */
@Singleton
class SearchHistoryStorage @Inject constructor(
    private val tokenStorage: TokenStorage,
    private val mmkv: MMKV,
    private val gson: Gson,
) {

    private val caches = mutableMapOf<Long?, MutableList<String>>()

    private fun cacheKey(): Long? = tokenStorage.currentUserId

    private fun history(): MutableList<String> {
        val key = cacheKey()
        return caches.getOrPut(key) { loadFromDisk(key) }
    }

    /// 读取全部历史，最新在前
    fun load(): List<String> = history().toList()

    /// 新增一条历史
    fun add(keyword: String) {
        val trimmed = keyword.trim()
        if (trimmed.isEmpty()) return
        val list = history()
        list.remove(trimmed)// 去重
        list.add(0, trimmed)
        if (list.size > MAX_SIZE) {
            caches[cacheKey()] = list.take(MAX_SIZE).toMutableList()
        }
        persist()
    }

    /// 删除指定历史记录
    fun remove(keyword: String) {
        if (history().remove(keyword)) persist()
    }

    /// 清空全部历史
    fun clear() {
        history().clear()
        persist()
    }

    private fun diskKey(uid: Long?): String =
        if (uid != null) "${KEY_SEARCH_HISTORY_PREFIX}_$uid" else KEY_SEARCH_HISTORY_PREFIX

    private fun loadFromDisk(uid: Long?): MutableList<String> {
        val json = mmkv.decodeString(diskKey(uid)) ?: return mutableListOf()
        return runCatching {
            val type = object : TypeToken<List<String>>() {}.type
            gson.fromJson<List<String>>(json, type).toMutableList()
        }.getOrDefault(mutableListOf())
    }

    private fun persist() {
        mmkv.encode(diskKey(cacheKey()), gson.toJson(history()))
    }

    private companion object {
        const val KEY_SEARCH_HISTORY_PREFIX = "search_history"
        const val MAX_SIZE = 20// 缓存上限
    }
}
