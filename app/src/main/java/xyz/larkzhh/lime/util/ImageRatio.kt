package xyz.larkzhh.lime.util

import android.graphics.BitmapFactory
import java.io.File

/// 读取本地图片宽高比
fun imageAspectRatio(file: File): Float {
    val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    runCatching { BitmapFactory.decodeFile(file.absolutePath, opts) }
    return if (opts.outWidth > 0 && opts.outHeight > 0) {
        opts.outWidth.toFloat() / opts.outHeight.toFloat()
    } else {
        1f
    }
}
