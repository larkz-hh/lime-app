package xyz.larkzhh.lime.util

import android.annotation.SuppressLint
import android.content.Context
import org.json.JSONObject

/// 离线语言包信息工具类
object TranslateModelInfo {

    private const val FALLBACK_DOWNLOAD_BYTES = 35_347_571L

    /// 获取下载体积
    @SuppressLint("DiscouragedApi")
    fun downloadSizeBytes(context: Context): Long = try {
        // 按名字运行时查找
        val id = context.resources.getIdentifier(
            "translate_models_metadata",
            "raw",
            context.packageName,
        )
        if (id == 0) throw IllegalStateException("translate_models_metadata 资源不存在")
        val text = context.resources.openRawResource(id).bufferedReader().use { it.readText() }
        JSONObject(text)
            .getJSONObject("PKG_HIGH")
            .getJSONObject("en_zh")
            .getLong("DL_SZ")
    } catch (e: Exception) {
        FALLBACK_DOWNLOAD_BYTES
    }

    /// MB 格式
    fun downloadSizeLabel(context: Context): String =
        "%.1fMB".format(downloadSizeBytes(context) / 1_000_000.0)
}
