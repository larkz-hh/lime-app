package xyz.larkzhh.lime.data.repository

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import xyz.larkzhh.lime.data.network.ApiService
import xyz.larkzhh.lime.data.network.model.NoteImageRequest
import xyz.larkzhh.lime.data.network.model.PublishNoteRequest
import xyz.larkzhh.lime.domain.repository.NoteRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NoteRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
    @param:ApplicationContext private val context: Context,
) : NoteRepository {

    /// 上传笔记图片
    override suspend fun uploadImage(uri: Uri): Result<String> = runCatching {
        val bytes = context.contentResolver.openInputStream(uri)?.readBytes()
            ?: error("无法读取图片文件")
        val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
        val ext = when (mimeType) {
            "image/png" -> "png"
            "image/webp" -> "webp"
            "image/gif" -> "gif"
            else -> "jpg"
        }
        val requestBody = bytes.toRequestBody(mimeType.toMediaTypeOrNull())
        val part = MultipartBody.Part.createFormData("file", "upload.$ext", requestBody)
        val response = apiService.uploadNoteImage(part)
        check(response.code == 200 && response.data != null) { response.message }
        response.data.url
    }

    /// 发布笔记
    override suspend fun publishNote(
        title: String?,
        content: String?,
        imageUrls: List<String>,
    ): Result<Unit> = runCatching {
        val images = imageUrls.mapIndexed { index, url ->
            NoteImageRequest(url = url, sortOrder = index)
        }
        val request = PublishNoteRequest(
            title = title?.ifBlank { null },
            content = content?.ifBlank { null },
            images = images,
        )
        val response = apiService.publishNote(request)
        check(response.code == 200) { response.message }
    }
}
