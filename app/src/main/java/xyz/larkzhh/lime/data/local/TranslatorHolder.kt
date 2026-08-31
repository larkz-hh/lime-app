package xyz.larkzhh.lime.data.local

import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import xyz.larkzhh.lime.util.LruCache
import javax.inject.Inject
import javax.inject.Singleton

/// 离线翻译仓库
@Singleton
class TranslatorHolder @Inject constructor() {

    // 翻译器实例缓存
    private val translators = object : LruCache<Pair<String, String>, Translator>(4) {
        override fun entryEvicted(key: Pair<String, String>, value: Translator) = value.close()
    }

    // 翻译结果缓存
    private val resultCache = LruCache<String, String>(256)

    private fun translatorFor(sourceTag: String, targetTag: String): Translator =
        translators.getOrPut(sourceTag to targetTag) {
            val options = TranslatorOptions.Builder()
                .setSourceLanguage(sourceTag)
                .setTargetLanguage(targetTag)
                .build()
            Translation.getClient(options)
        }

    /// 确认离线包可用
    suspend fun ensureModel(
        sourceTag: String,
        targetTag: String,
        conditions: DownloadConditions = DownloadConditions.Builder().build(),
    ) {
        translatorFor(sourceTag, targetTag)
            .downloadModelIfNeeded(conditions)
            .await()
    }

    /// 翻译文本
    suspend fun translate(text: String, sourceTag: String, targetTag: String): String {
        val key = "$sourceTag|$targetTag|$text"
        resultCache[key]?.let { return it }
        val result = withContext(Dispatchers.IO) {
            translatorFor(sourceTag, targetTag).translate(text).await()
        }
        resultCache[key] = result
        return result
    }
}
