package xyz.larkzhh.lime.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.exifinterface.media.ExifInterface
import android.media.MediaMetadataRetriever
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/// 视频尺寸信息
data class VideoDimensions(val width: Int, val height: Int)

/// 读取视频的显示宽高
suspend fun readVideoDimensions(context: Context, uri: Uri): VideoDimensions =
    withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()// 创建检索器
        try {
            retriever.setDataSource(context, uri)
            val w = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
                ?.toIntOrNull() ?: 0
            val h = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
                ?.toIntOrNull() ?: 0
            val rotation = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)
                ?.toIntOrNull() ?: 0
            if (rotation == 90 || rotation == 270) {
                VideoDimensions(width = h, height = w)// 旋转情况交换宽高
            } else {
                VideoDimensions(width = w, height = h)
            }
        } catch (_: Exception) {
            VideoDimensions(0, 0)
        } finally {
            retriever.release()
        }
    }

/// 截取指定时间点的帧并保存为 JPEG
suspend fun extractFrameToCache(context: Context, uri: Uri, timeMs: Long): Uri? =
    withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        var bitmap: Bitmap? = null
        try {
            retriever.setDataSource(context, uri)
            // 截取指定时间点的帧
            bitmap = retriever.getFrameAtTime(
                timeMs * 1000L, // 微秒
                MediaMetadataRetriever.OPTION_CLOSEST_SYNC,// 取关键帧
            ) ?: return@withContext null
            val file = File(context.cacheDir, "cover_frame_${timeMs}.jpg")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }
            Uri.fromFile(file)
        } catch (_: Exception) {
            null
        } finally {
            bitmap?.recycle()
            retriever.release()
        }
    }

/// 读取图片的像素宽高
suspend fun readImageDimensions(context: Context, uri: Uri): VideoDimensions =
    withContext(Dispatchers.IO) {
        var w = 0
        var h = 0
        var rotate = false
        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeStream(input, null, options)
                w = options.outWidth
                h = options.outHeight
            }
            try {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    val orientation = ExifInterface(input).getAttributeInt(
                        ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL,
                    )
                    rotate = orientation == ExifInterface.ORIENTATION_ROTATE_90 ||
                        orientation == ExifInterface.ORIENTATION_ROTATE_270
                }
            } catch (_: Exception) { }
            if (rotate) VideoDimensions(width = h, height = w) else VideoDimensions(width = w, height = h)
        } catch (_: Exception) {
            VideoDimensions(0, 0)
        }
    }
