package xyz.larkzhh.lime.data.local

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.vosk.Model
import org.vosk.Recognizer
import xyz.larkzhh.lime.util.text.SpeechText
import javax.inject.Inject
import javax.inject.Singleton

private const val UNKNOWN_MS = -1L
/// Vosk 认不出来的词
private const val UNK_TOKEN = "[unk]"

private const val KEY_RESULT = "result"
private const val KEY_WORD = "word"
private const val KEY_START = "start"
private const val KEY_END = "end"
private const val KEY_TEXT = "text"
private const val KEY_PARTIAL = "partial"

/**
 * 端侧语音识别内核
 * 一次手势对应一个 Session，按顺序给 PCM 获取增量文本
 */
@Singleton
class SpeechEngine @Inject constructor(
    private val store: SpeechModelStore,
) {

    private val lock = Mutex()
    private var model: Model? = null

    /// 语音包是否就绪
    fun isReady(): Boolean = store.isReady()

    /// 打开一次识别会话
    suspend fun openSession(): Session? = lock.withLock {
        if (!store.isReady()) return@withLock null
        withContext(Dispatchers.Default) {
            try {
                val loaded = model ?: Model(store.modelDir.absolutePath).also { model = it }
                val recognizer = Recognizer(loaded, SAMPLE_RATE.toFloat())
                runCatching { recognizer.setWords(true) }// 词级时间戳
                Session(recognizer)
            } catch (_: Exception) {
                null
            }
        }
    }

    /// 释放模型
    suspend fun release() = lock.withLock {
        withContext(Dispatchers.Default) {
            runCatching { model?.close() }
            model = null
        }
    }

    /**
     * 一次识别会话
     */
    class Session internal constructor(
        private val recognizer: Recognizer,
    ) {
        private val words = mutableListOf<SpeechText.Word>()// 已确认的词
        private var partial = ""// 还没确认的临时文本
        fun text(): String = SpeechText.format(words, tail = partial) /// 当前文本

        /// 喂一段 PCM，返回当前文本
        fun accept(pcm: ByteArray, length: Int): String {
            if (length <= 0) return text()
            try {
                if (recognizer.acceptWaveForm(pcm, length)) {
                    append(recognizer.result)
                    partial = ""
                } else {
                    partial = extract(recognizer.partialResult, KEY_PARTIAL)
                }
            } catch (_: Exception) { }
            return text()
        }

        /// 结束并给出最终文本
        fun finish(): String {
            val flushed = try {
                append(recognizer.finalResult)
            } catch (_: Exception) {
                false
            }
            return SpeechText.format(words, tail = if (flushed) "" else partial, final = true)
        }

        fun close() {
            runCatching { recognizer.close() }
        }

        /// 解析 JSON 后追加
        private fun append(json: String): Boolean {
            val obj = runCatching { JSONObject(json) }.getOrNull() ?: return false
            val array = obj.optJSONArray(KEY_RESULT)
            if (array != null && array.length() > 0) {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    val word = item.optString(KEY_WORD, "").trim()
                    if (word.isEmpty() || word == UNK_TOKEN) continue
                    words.add(
                        SpeechText.Word(
                            text = word,
                            startMs = toMillis(item.optDouble(KEY_START, -1.0)),
                            endMs = toMillis(item.optDouble(KEY_END, -1.0)),
                        ),
                    )
                }
                return true
            }
            val text = obj.optString(KEY_TEXT, "").trim()
            if (text.isEmpty()) return false
            words.add(SpeechText.untimed(text))
            return true
        }

        private fun extract(json: String, key: String): String =
            runCatching { JSONObject(json).optString(key, "") }.getOrDefault("")

        /// 秒转毫秒
        private fun toMillis(value: Double): Long =
            if (value.isNaN() || value < 0) UNKNOWN_MS else (value * 1000).toLong()
    }

    companion object {
        const val SAMPLE_RATE = 16_000
    }
}
