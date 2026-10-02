package xyz.larkzhh.lime.data.local

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit
import java.util.zip.ZipInputStream
import javax.inject.Inject
import javax.inject.Singleton
import xyz.larkzhh.lime.BuildConfig

/// 语音包状态
enum class SpeechPackStatus { Checking, NotDownloaded, Downloading, Downloaded, Failed }

/// 语音包状态与下载进度
data class SpeechPackUiState(
    val status: SpeechPackStatus = SpeechPackStatus.Checking,
    val progress: Int = 0,
)

/**
 * 端侧语音识别模型包
 */
@Singleton
class SpeechModelStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    val state: StateFlow<SpeechPackUiState> = sharedState.asStateFlow()// 下载状态
    val modelDir: File get() = File(context.filesDir, "$PACK_DIR/$MODEL_NAME")// 模型目录
    private val stagingDir: File get() = File(context.filesDir, "$PACK_DIR/$MODEL_NAME-tmp")// 临时目录
    private val partFile: File get() = File(context.cacheDir, "$ZIP_NAME.part")// 部分下载文件
    private val metaFile: File get() = File(context.cacheDir, "$ZIP_NAME.meta")// 部分下载文件来源信息
    fun isReady(): Boolean = File(modelDir, READY_MARKER).exists()

    /// 检查本地语音包
    suspend fun refresh() {
        val ready = withContext(Dispatchers.IO) { isReady() }
        when {
            ready -> sharedState.value = SpeechPackUiState(SpeechPackStatus.Downloaded, 100)
            sharedState.value.status == SpeechPackStatus.Downloading -> Unit
            else -> sharedState.value = SpeechPackUiState(SpeechPackStatus.NotDownloaded)
        }
    }

    /// 下载并解压语音包
    suspend fun download(): Boolean = sharedDownloadLock.withLock {
        withContext(Dispatchers.IO) {
            if (isReady()) {
                sharedState.value = SpeechPackUiState(SpeechPackStatus.Downloaded, 100)
                return@withContext true
            }
            sharedState.value = SpeechPackUiState(SpeechPackStatus.Downloading, 0)
            try {
                downloadZip()
                try {
                    unzip(partFile, stagingDir)
                    if (!File(stagingDir, READY_MARKER).exists()) throw IOException("语音包内容不完整")
                } catch (e: Exception) {
                    resetPart()
                    throw e
                }
                resetPart()
                modelDir.deleteRecursively()
                modelDir.parentFile?.mkdirs()
                if (!stagingDir.renameTo(modelDir)) {
                    stagingDir.copyRecursively(modelDir, overwrite = true)
                }
                sharedState.value = SpeechPackUiState(SpeechPackStatus.Downloaded, 100)
                true
            } catch (ce: CancellationException) {
                // 保留片段，下次续传
                sharedState.value = SpeechPackUiState(SpeechPackStatus.NotDownloaded)
                throw ce
            } catch (_: Exception) {
                sharedState.value = SpeechPackUiState(SpeechPackStatus.Failed)
                false
            } finally {
                stagingDir.deleteRecursively()
            }
        }
    }

    /// 删除本地语音包
    suspend fun delete() {
        withContext(Dispatchers.IO) {
            modelDir.deleteRecursively()
            stagingDir.deleteRecursively()
            resetPart()
        }
        sharedState.value = SpeechPackUiState(SpeechPackStatus.NotDownloaded)
    }

    /// 下载 zip 到缓存目录
    private fun downloadZip() {
        var restarts = 0
        while (true) {
            val existing = if (partFile.exists()) partFile.length() else 0L
            val meta = readMeta()
            if (existing > 0 && meta?.etag != null) {
                val remoteEtag = probeEtag()
                if (remoteEtag != null && remoteEtag != meta.etag) {
                    resetPart()
                    continue
                }
            }
            val request = Request.Builder()
                .url(BuildConfig.SPEECH_MODEL_URL)
                .apply { if (existing > 0) header("Range", "bytes=$existing-") }
                .build()
            var restart = false
            client.newCall(request).execute().use { response ->
                when {
                    response.code == HTTP_RANGE_NOT_SATISFIABLE -> {
                        if (existing > 0 && meta?.total == existing) return
                        resetPart()
                        restart = true
                    }

                    // 服务端不支持续传，从头写
                    existing > 0 && response.code == HTTP_OK -> {
                        resetPart()
                        restart = true
                    }

                    response.code == HTTP_PARTIAL || response.code == HTTP_OK -> writeZipBody(response, existing)

                    else -> throw IOException("HTTP ${response.code}")
                }
            }
            if (!restart) return
            restarts++
            if (restarts > MAX_RESTARTS) throw IOException("下载失败")
        }
    }

    /// 写入响应体，同步进度
    private fun writeZipBody(response: Response, offset: Long) {
        val body = response.body ?: throw IOException("响应为空")
        val append = offset > 0 && response.code == HTTP_PARTIAL
        val total = parseContentRangeTotal(response.header("Content-Range"))
            ?: (offset + body.contentLength()).takeIf { it > 0 }
            ?: -1L
        writeMeta(response.header("ETag"), total)
        partFile.parentFile?.mkdirs()
        var done = if (append) offset else 0L
        body.byteStream().use { input ->
            FileOutputStream(partFile, append).buffered().use { output ->
                val buffer = ByteArray(64 * 1024)
                var lastPercent = -1
                while (true) {
                    val read = input.read(buffer)
                    if (read <= 0) break
                    output.write(buffer, 0, read)
                    done += read
                    if (total > 0) {
                        val percent = (done * 100 / total).toInt().coerceIn(0, 99)
                        if (percent != lastPercent) {
                            lastPercent = percent
                            sharedState.value = SpeechPackUiState(SpeechPackStatus.Downloading, percent)
                        }
                    }
                }
            }
        }
        if (total > 0 && partFile.length() < total) throw IOException("下载未完成")
    }

    ///  探测远端 ETag
    private fun probeEtag(): String? = runCatching {
        client.newCall(Request.Builder().url(BuildConfig.SPEECH_MODEL_URL).head().build())
            .execute()
            .use { it.header("ETag") }
    }.getOrNull()

    /// 解析总长度 Content-Range: bytes 100-999/1000 -> 1000
    private fun parseContentRangeTotal(contentRange: String?): Long? =
        contentRange?.substringAfterLast('/', "")
            ?.trim()
            ?.toLongOrNull()
            ?.takeIf { it > 0 }

    /// 读取续传元数据
    private fun readMeta(): PartMeta? = runCatching {
        if (!metaFile.exists()) return@runCatching null
        val lines = metaFile.readLines()
        PartMeta(
            etag = lines.getOrNull(0)?.takeIf { it.isNotBlank() },
            total = lines.getOrNull(1)?.toLongOrNull() ?: -1L,
        )
    }.getOrNull()

    /// 写入续传元数据
    private fun writeMeta(etag: String?, total: Long) {
        runCatching { metaFile.writeText("${etag ?: ""}\n$total") }
    }

    private fun resetPart() {
        partFile.delete()
        metaFile.delete()
    }

    /// 解压
    private fun unzip(zip: File, target: File) {
        target.deleteRecursively()
        target.mkdirs()
        val root = target.canonicalPath + File.separator
        ZipInputStream(zip.inputStream().buffered()).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                val relative = entry.name.substringAfter('/', "")
                if (relative.isNotBlank()) {
                    val out = File(target, relative)
                    if (out.canonicalPath.startsWith(root)) {
                        if (entry.isDirectory) {
                            out.mkdirs()
                        } else {
                            out.parentFile?.mkdirs()
                            out.outputStream().buffered().use { os -> zis.copyTo(os, 64 * 1024) }
                        }
                    }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
    }

    /// 来源信息
    private data class PartMeta(val etag: String?, val total: Long)

    companion object {
        private const val PACK_DIR = "speech-pack"
        private const val MODEL_NAME = "vosk-model-small-cn"
        private const val READY_MARKER = "am/final.mdl"
        private const val ZIP_NAME = "speech-pack.zip"
        private const val HTTP_OK = 200
        private const val HTTP_PARTIAL = 206
        private const val HTTP_RANGE_NOT_SATISFIABLE = 416
        private const val MAX_RESTARTS = 2
        private val sharedState = MutableStateFlow(SpeechPackUiState())
        private val sharedDownloadLock = Mutex()
    }
}
