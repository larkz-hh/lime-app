package xyz.larkzhh.lime.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import coil3.BitmapImage
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import androidx.core.graphics.get

/// 取背景图底部区域的代表色
suspend fun extractGradientColor(context: Context, imageUrl: String): Int? {
    val request = ImageRequest.Builder(context)
        .data(imageUrl)
        .allowHardware(false)
        .build()
    val result = SingletonImageLoader.get(context).execute(request)
    if (result !is SuccessResult) return null
    val bitmap = (result.image as? BitmapImage)?.bitmap ?: return null
    return runCatching {
        moodColor(bottomAverageColor(bitmap))
    }.getOrNull()
}

// 取底部高度内平均色
private fun bottomAverageColor(bitmap: Bitmap, fraction: Float = 0.15f): Int {
    val bandHeight = (bitmap.height * fraction).toInt().coerceAtLeast(1)// 取图片最底部15%的高度
    val startY = bitmap.height - bandHeight
    val stepX = (bitmap.width / 48).coerceAtLeast(1)
    val stepY = (bandHeight / 12).coerceAtLeast(1)// 抽样步长
    // 累加 RGB 通道值
    var r = 0L
    var g = 0L
    var b = 0L
    var count = 0
    // 抽样便利
    for (y in startY until bitmap.height step stepY) {
        for (x in 0 until bitmap.width step stepX) {
            val pixel = bitmap[x, y]// 取出指定坐标的像素
            r += AndroidColor.red(pixel)
            g += AndroidColor.green(pixel)
            b += AndroidColor.blue(pixel)
            count++
        }
    }
    return AndroidColor.rgb((r / count).toInt(), (g / count).toInt(), (b / count).toInt())// 求平均值
}

private fun moodColor(rgb: Int): Int {
    val hsv = FloatArray(3)
    AndroidColor.colorToHSV(rgb, hsv)// 把颜色从 RGB 格式转换成 HSV 格式
    hsv[1] = (hsv[1] * 1.25f).coerceAtMost(0.6f)// 饱和度
    hsv[2] = hsv[2] * 0.5f// 亮度
    return AndroidColor.HSVToColor(hsv)
}
