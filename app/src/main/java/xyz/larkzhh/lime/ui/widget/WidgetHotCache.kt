package xyz.larkzhh.lime.ui.widget

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.tencent.mmkv.MMKV
import xyz.larkzhh.lime.data.network.model.HotSearchItem

object WidgetHotCache {

    /// 热搜最多展示条数
    const val MAX_HOT_COUNT = 6

    /// 联网超时
    const val FETCH_TIMEOUT_MS = 4_000L

    private const val KEY_HOT = "widget_hot_searches"
    private const val KEY_LAST_FETCH = "widget_hot_last_fetch_ms"
    private const val REFRESH_INTERVAL_MS = 10 * 60 * 1000L

    private val gson = Gson()

    // 读取缓存
    fun read(): List<HotSearchItem> {
        val json = MMKV.defaultMMKV().decodeString(KEY_HOT) ?: return emptyList()
        return runCatching {
            gson.fromJson<List<HotSearchItem>>(
                json,
                object : TypeToken<List<HotSearchItem>>() {}.type
            )
        }.getOrDefault(emptyList())
    }

    /// 写入缓存
    fun write(items: List<HotSearchItem>) {
        MMKV.defaultMMKV().encode(KEY_HOT, gson.toJson(items))
        MMKV.defaultMMKV().encode(KEY_LAST_FETCH, System.currentTimeMillis())
    }

    /// 是否刷新
    fun shouldRefresh(): Boolean {
        val last = MMKV.defaultMMKV().decodeLong(KEY_LAST_FETCH, 0L)
        return System.currentTimeMillis() - last >= REFRESH_INTERVAL_MS
    }
}
