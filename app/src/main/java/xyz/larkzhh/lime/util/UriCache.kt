package xyz.larkzhh.lime.util

import android.content.Context
import android.net.Uri
import java.io.File

/// 将相册等 content Uri 拷贝到应用缓存目录
fun Context.copyUriToCache(uri: Uri, prefix: String): String? = runCatching {
    val mime = contentResolver.getType(uri)
    val ext = when (mime) {
        "image/png" -> "png"
        "image/webp" -> "webp"
        "image/gif" -> "gif"
        else -> "jpg"
    }
    val file = File(cacheDir, "${prefix}_${System.currentTimeMillis()}.$ext")
    contentResolver.openInputStream(uri)?.use { input ->
        file.outputStream().use { output -> input.copyTo(output) }
    }
    file.absolutePath
}.getOrNull()
