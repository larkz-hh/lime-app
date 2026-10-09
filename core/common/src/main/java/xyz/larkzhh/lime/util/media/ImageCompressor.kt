package xyz.larkzhh.lime.util.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import androidx.core.graphics.scale

/// 上传前图片压缩结果
data class CompressedImage(
    val bytes: ByteArray,
    val mimeType: String,
    val ext: String,
) {
    // 内容比较
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CompressedImage) return false
        return bytes.contentEquals(other.bytes) &&
            mimeType == other.mimeType &&
            ext == other.ext
    }

    override fun hashCode(): Int {
        var result = bytes.contentHashCode()
        result = 31 * result + mimeType.hashCode()
        result = 31 * result + ext.hashCode()
        return result
    }
}

/**
 * 图片压缩工具
 * - 上传前降采样、压缩
 * - 长边最大 1280px，质量 85
 * - PNG / WebP 转 WebP ，JPEG 转 JPEG，GIF 保持原样
 */
object ImageCompressor {

    private const val MAX_DIMENSION = 1280
    private const val QUALITY = 85

    /// 压缩图片
    suspend fun compress(
        context: Context,
        uri: Uri,
        maxDimension: Int = MAX_DIMENSION,
    ): CompressedImage =
        withContext(Dispatchers.IO) {
            // 读取原始字节和 MIME 类型
            val original = context.contentResolver.openInputStream(uri)?.readBytes()
                ?: error("无法读取图片文件")
            val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
            // GIF 动图不压缩
            if (mimeType == "image/gif") {
                return@withContext CompressedImage(original, "image/gif", "gif")
            }
            // 解析尺寸
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(original, 0, original.size, bounds)
            val width = bounds.outWidth
            val height = bounds.outHeight
            if (width <= 0 || height <= 0) {
                return@withContext CompressedImage(original, mimeType, extOf(mimeType))
            }
            // 按采样率解码
            val decoded = BitmapFactory.decodeByteArray(
                original, 0, original.size,
                BitmapFactory.Options().apply { inSampleSize = sampleSize(width, height, maxDimension) },
            ) ?: error("无法解析图片")

            // 缩放到目标长边
            val scaled = scaleToMax(decoded, maxDimension)

            // 选择编码格式
            val (format, outMime, outExt) = if (mimeType == "image/png" || mimeType == "image/webp") {
                Triple(Bitmap.CompressFormat.WEBP, "image/webp", "webp")
            } else {
                Triple(Bitmap.CompressFormat.JPEG, "image/jpeg", "jpg")
            }

            // 质量压缩，输出字节流
            val out = ByteArrayOutputStream()
            scaled.compress(format, QUALITY, out)
            if (scaled !== decoded) scaled.recycle()// 释放内存
            decoded.recycle()

            CompressedImage(out.toByteArray(), outMime, outExt)
        }

    /// 计算采样率
    private fun sampleSize(width: Int, height: Int, maxDimension: Int): Int {
        var sample = 1
        var maxSide = maxOf(width, height)
        while (maxSide / 2 >= maxDimension) {
            sample *= 2
            maxSide /= 2
        }
        return sample
    }

    /// 按比例缩放图片
    private fun scaleToMax(bitmap: Bitmap, maxDim: Int): Bitmap {
        val larger = maxOf(bitmap.width, bitmap.height)
        if (larger <= maxDim) return bitmap
        val ratio = maxDim.toFloat() / larger
        return bitmap.scale(
            (bitmap.width * ratio).toInt().coerceAtLeast(1),
            (bitmap.height * ratio).toInt().coerceAtLeast(1),
        )
    }

    /// MIME 类型转扩展名
    private fun extOf(mimeType: String): String = when (mimeType) {
        "image/png" -> "png"
        "image/webp" -> "webp"
        else -> "jpg"
    }
}
