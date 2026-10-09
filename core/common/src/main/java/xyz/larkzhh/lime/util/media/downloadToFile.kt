package xyz.larkzhh.lime.util.media

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/// 把 url 下载到指定文件
suspend fun downloadToFile(url: String, destFile: File): Boolean =
    withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            connection = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 20_000
                readTimeout = 30_000
                instanceFollowRedirects = true
                connect()
            }
            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                return@withContext false
            }
            connection.inputStream.use { input ->
                destFile.outputStream().use { out -> input.copyTo(out) }
            }
            true
        } catch (_: Exception) {
            false
        } finally {
            connection?.disconnect()
        }
    }
