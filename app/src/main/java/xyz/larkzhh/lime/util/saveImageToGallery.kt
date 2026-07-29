package xyz.larkzhh.lime.util

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import coil3.BitmapImage
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

suspend fun saveImageToGallery(context: Context, url: String): Boolean =
    withContext(Dispatchers.IO) {
        try {
            val imageLoader = SingletonImageLoader.get(context)
            val request = ImageRequest.Builder(context)// 构建请求
                .data(url)
                .allowHardware(false)// 禁用硬件位图
                .build()
            val result = imageLoader.execute(request)
            val bitmap = ((result as? SuccessResult)?.image as? BitmapImage)?.bitmap
                ?: return@withContext false

            val filename = "lime_${System.currentTimeMillis()}.jpg"
            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, filename)// 设置图片在相册里显示的文件名
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")// 声明文件的 MIME 类型
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Lime")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                } else {
                    @Suppress("DEPRECATION")
                    put(
                        MediaStore.Images.Media.DATA,
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                            .absolutePath + "/Lime/" + filename,
                    )
                }
            }

            val uri = context.contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues,
            ) ?: return@withContext false

            context.contentResolver.openOutputStream(uri)?.use { stream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, stream)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)// 公开发布
                context.contentResolver.update(uri, contentValues, null, null)// 更新
            }

            true
        } catch (_: Exception) {
            false
        }
    }
