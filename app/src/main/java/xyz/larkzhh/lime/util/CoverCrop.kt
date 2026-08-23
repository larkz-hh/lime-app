package xyz.larkzhh.lime.util

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import coil3.BitmapImage
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.roundToInt

/// 封面裁剪矩形，源图像素坐标
data class CropRectPx(val left: Int, val top: Int, val width: Int, val height: Int)


/// 计算基准裁剪尺寸
private fun coverCropBase(imgW: Int, imgH: Int, ratio: Float): Pair<Float, Float> {
    val imgRatio = imgW.toFloat() / imgH
    return if (imgRatio >= ratio) {
        (imgH * ratio) to imgH.toFloat() // 高度撑满
    } else {
        imgW.toFloat() to (imgW / ratio) // 宽度撑满
    }
}

/// 由缩放与焦点推导源图上的裁剪矩形
fun computeCropRect(
    imgW: Int,
    imgH: Int,
    ratio: Float,
    scale: Float,// 相对覆盖填满的缩放
    focusX: Float,
    focusY: Float,
): CropRectPx {
    val (cropW0, cropH0) = coverCropBase(imgW, imgH, ratio)// 获取基准尺寸
    // 根据缩放比例计算实际裁剪尺寸
    val cropW = (cropW0 / scale).coerceIn(1f, imgW.toFloat())
    val cropH = (cropH0 / scale).coerceIn(1f, imgH.toFloat())
    // 根据焦点位置计算裁剪框的左上角坐标
    val leftF = (focusX * imgW - cropW / 2f).coerceIn(0f, (imgW - cropW).coerceAtLeast(0f))
    val topF = (focusY * imgH - cropH / 2f).coerceIn(0f, (imgH - cropH).coerceAtLeast(0f))

    return CropRectPx(
        left = leftF.roundToInt(),
        top = topF.roundToInt(),
        width = cropW.roundToInt().coerceAtLeast(1),
        height = cropH.roundToInt().coerceAtLeast(1),
    )
}

/// 用 Coil 解码源图，按裁剪变换切出封面并写入缓存
suspend fun cropCoverToCache(
    context: Context,
    request: ImageRequest,
    ratio: Float,
    scale: Float,
    focusX: Float,
    focusY: Float,
): Uri? = withContext(Dispatchers.IO) {
    var out: Bitmap? = null
    try {
        val loader = SingletonImageLoader.get(context)
        val result = loader.execute(request)
        val src = ((result as? SuccessResult)?.image as? BitmapImage)?.bitmap
            ?: return@withContext null

        val rect = computeCropRect(src.width, src.height, ratio, scale, focusX, focusY)
        val w = rect.width.coerceAtMost(src.width - rect.left)
        val h = rect.height.coerceAtMost(src.height - rect.top)
        out = Bitmap.createBitmap(src, rect.left, rect.top, w, h)
        // 写入缓存
        val file = File(context.cacheDir, "cover_crop_${System.currentTimeMillis()}.jpg")
        FileOutputStream(file).use { o ->
            out.compress(Bitmap.CompressFormat.JPEG, 90, o)
        }
        Uri.fromFile(file)
    } catch (_: Exception) {
        null
    } finally {
        out?.recycle()
    }
}

