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
class SearchHistoryStorage @Inject constructor() {

    private val mmkv by lazy { MMKV.defaultMMKV() }
    private val gson by lazy { Gson() }

    /// 内存中的历史列表
    private var history: MutableList<String> = loadFromDisk()

    /// 读取全部历史，最新在前
    fun load(): List<String> = history.toList()

    /// 新增一条历史
    fun add(keyword: String) {
        val trimmed = keyword.trim()
        if (trimmed.isEmpty()) return
        history.remove(trimmed)// 去重
        history.add(0, trimmed)
        if (history.size > MAX_SIZE) {
            history = history.take(MAX_SIZE).toMutableList()
        }
        persist()
    }

    /// 删除指定历史记录
    fun remove(keyword: String) {
        if (history.remove(keyword)) persist()
    }

    /// 清空全部历史
    fun clear() {
        history.clear()
        persist()
    }

    private fun loadFromDisk(): MutableList<String> {
        val json = mmkv.decodeString(KEY_SEARCH_HISTORY) ?: return mutableListOf()
        return runCatching {
            val type = object : TypeToken<List<String>>() {}.type
            gson.fromJson<List<String>>(json, type).toMutableList()
        }.getOrDefault(mutableListOf())
    }

    private fun persist() {
        mmkv.encode(KEY_SEARCH_HISTORY, gson.toJson(history))
    }

    private companion object {
        const val KEY_SEARCH_HISTORY = "search_history"
        const val MAX_SIZE = 20// 缓存上限
    }
}
