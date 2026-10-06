package xyz.larkzhh.lime.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.net.Uri
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.google.zxing.qrcode.encoder.ByteMatrix
import com.google.zxing.qrcode.encoder.Encoder
import androidx.core.graphics.createBitmap
import androidx.core.net.toUri
import androidx.core.graphics.scale
import androidx.core.graphics.withClip

/// 生成二维码位图
fun generateQrBitmap(
    content: String,
    sizePx: Int = 512,
    margin: Int = 4,
): Bitmap? {
    val matrix = encodeMatrix(content, ErrorCorrectionLevel.M) ?: return null
    return drawQrCode(
        matrix = matrix,
        sizePx = sizePx,
        margin = margin,
        startColor = Color.BLACK,
        endColor = Color.BLACK,
        logo = null,
    )
}

/**
 * 生成二维码
 * @param startColor 渐变起始色
 * @param endColor  渐变结束色
 * @param logo  中心图
 */
fun generateGradientQrBitmap(
    content: String,
    sizePx: Int = 512,
    startColor: Int = Color.rgb(0x6F, 0xC6, 0xE9),
    endColor: Int = Color.rgb(0xA2, 0x9B, 0xEA),
    logo: Bitmap? = null,
    margin: Int = 4,
): Bitmap? {
    val matrix = encodeMatrix(content, ErrorCorrectionLevel.H) ?: return null
    return drawQrCode(
        matrix = matrix,
        sizePx = sizePx,
        margin = margin,
        startColor = startColor,
        endColor = endColor,
        logo = logo,
    )
}

/// zxing 编码
private fun encodeMatrix(content: String, ecLevel: ErrorCorrectionLevel): ByteMatrix? =
    runCatching { Encoder.encode(content, ecLevel).matrix }.getOrNull()

/// 绘制
private fun drawQrCode(
    matrix: ByteMatrix,
    sizePx: Int,
    margin: Int,
    startColor: Int,
    endColor: Int,
    logo: Bitmap?,
): Bitmap? {
    val n = matrix.width
    if (n <= 0 || sizePx <= 0) return null
    val cell = sizePx.toFloat() / (n + 2 * margin)
    val bitmap = createBitmap(sizePx, sizePx)
    val canvas = Canvas(bitmap)
    canvas.drawColor(Color.WHITE)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    // 三个定位角范围
    fun isFinder(x: Int, y: Int): Boolean =
        (x < 7 && y < 7) || (x >= n - 7 && y < 7) || (x < 7 && y >= n - 7)

    // 是否渐变
    val isGradient = startColor != endColor

    for (y in 0 until n) {
        for (x in 0 until n) {
            if (matrix.get(x, y).toInt() != 1) continue
            val left = (margin + x) * cell
            val top = (margin + y) * cell
            // 横向渐变进度
            val progress = x.toFloat() / (n - 1).coerceAtLeast(1)
            if (isFinder(x, y)) {
                if (!isGradient) {
                    paint.color = lerpColor(startColor, endColor, progress)
                    canvas.drawRect(left, top, left + cell, top + cell, paint)
                }
            } else if (!isGradient) {
                paint.color = lerpColor(startColor, endColor, progress)
                canvas.drawRect(left, top, left + cell, top + cell, paint)
            } else {
                paint.color = lerpColor(startColor, endColor, progress)
                val radius = cell * 0.40f
                canvas.drawCircle(left + cell / 2f, top + cell / 2f, radius, paint)
            }
        }
    }

    // 三个定位角
    if (isGradient) {
        val corners = arrayOf(
            margin + 3.5f to margin + 3.5f,// 左上
            margin + n - 3.5f to margin + 3.5f,// 右上
            margin + 3.5f to margin + n - 3.5f,// 左下
        )
        for ((cxM, cyM) in corners) {
            val prog = (cxM / (n - 1).coerceAtLeast(1)).coerceIn(0f, 1f)
            val color = lerpColor(startColor, endColor, prog)
            val px = cxM * cell
            val py = cyM * cell
            paint.color = color
            canvas.drawCircle(px, py, 3.5f * cell, paint)// 外环
            paint.color = Color.WHITE
            canvas.drawCircle(px, py, 2.4f * cell, paint)// 白环
            paint.color = color
            canvas.drawCircle(px, py, 1.4f * cell, paint)// 内芯
        }
    }

    // 中心 logo
    if (logo != null) {
        val logoSize = sizePx * 0.20f
        val pad = logoSize * 0.10f
        val cx = sizePx / 2f
        val cy = sizePx / 2f
        val bg = RectF(cx - logoSize / 2f - pad, cy - logoSize / 2f - pad, cx + logoSize / 2f + pad, cy + logoSize / 2f + pad)
        paint.color = Color.WHITE
        canvas.drawRoundRect(bg, pad * 2, pad * 2, paint)

        val scaled = logo.scale(logoSize.toInt(), logoSize.toInt())
        val clip = Path().apply {
            addRoundRect(
                RectF(cx - logoSize / 2f, cy - logoSize / 2f, cx + logoSize / 2f, cy + logoSize / 2f),
                logoSize * 0.18f,
                logoSize * 0.18f,
                Path.Direction.CW,
            )
        }
        canvas.withClip(clip) {
            drawBitmap(scaled, cx - logoSize / 2f, cy - logoSize / 2f, null)
        }
    }
    return bitmap
}

/// 颜色线性插值
private fun lerpColor(from: Int, to: Int, t: Float): Int {
    val tt = t.coerceIn(0f, 1f)
    fun ch(shift: Int) = (((from shr shift) and 0xFF) + ((((to shr shift) and 0xFF) - ((from shr shift) and 0xFF)) * tt).toInt())
        .coerceIn(0, 255)
    return Color.rgb(ch(16), ch(8), ch(0))
}

// 用户二维码内容
fun limeUserQrContent(uid: String): String = "lime://user/${Uri.encode(uid)}"

/// 笔记分享二维码内容
fun limeNoteQrContent(noteId: Long): String = "lime://note/$noteId"

/// 视频分享二维码内容
fun limeVideoQrContent(noteId: Long): String = "lime://video/$noteId"

/// 解析用户名片二维码文本
fun parseLimeUserQr(raw: String): String? {
    val uri = runCatching { raw.toUri() }.getOrNull() ?: return null
    return if (uri.scheme == "lime" && uri.host == "user") {
        uri.pathSegments.firstOrNull()
    } else {
        null
    }
}

/// 解析笔记分享二维码文本
fun parseLimeNoteQr(raw: String): String? {
    val uri = runCatching { raw.toUri() }.getOrNull() ?: return null
    return if (uri.scheme == "lime" && uri.host == "note") {
        uri.pathSegments.firstOrNull()
    } else {
        null
    }
}

/// 解析视频分享二维码文本
fun parseLimeVideoQr(raw: String): String? {
    val uri = runCatching { raw.toUri() }.getOrNull() ?: return null
    return if (uri.scheme == "lime" && uri.host == "video") {
        uri.pathSegments.firstOrNull()
    } else {
        null
    }
}
