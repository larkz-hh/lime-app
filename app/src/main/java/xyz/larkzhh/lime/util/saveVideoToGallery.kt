package xyz.larkzhh.lime.util

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

suspend fun saveVideoToGallery(context: Context, url: String): Boolean =
    withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            val filename = "lime_${System.currentTimeMillis()}.mp4"
            val contentValues = ContentValues().apply {
                put(MediaStore.Video.Media.DISPLAY_NAME, filename)// 相册显示文件名
                put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")// MIME 类型
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_DCIM + "/Lime")
                    put(MediaStore.Video.Media.IS_PENDING, 1)
                } else {
                    @Suppress("DEPRECATION")
                    put(
                        MediaStore.Video.Media.DATA,
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
                            .absolutePath + "/Lime/" + filename,
                    )
                }
            }

            val uri = context.contentResolver.insert(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                contentValues,
            ) ?: return@withContext false

            // 下载视频流写入
            connection = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 15000
                readTimeout = 15000
                connect()
            }
            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                context.contentResolver.delete(uri, null, null)// 清理占位
                return@withContext false
            }

            context.contentResolver.openOutputStream(uri)?.use { out ->
                connection.inputStream.use { input ->
                    input.copyTo(out, bufferSize = 8 * 1024)
                }
            } ?: return@withContext false

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.put(MediaStore.Video.Media.IS_PENDING, 0)// 公开发布
                context.contentResolver.update(uri, contentValues, null, null)
            }

            true
        } catch (_: Exception) {
            false
        } finally {
            connection?.disconnect()
        }
    }
