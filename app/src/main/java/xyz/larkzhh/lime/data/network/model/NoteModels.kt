package xyz.larkzhh.lime.data.network.model

data class UploadNoteImageResponse(
    val url: String,
)

data class NoteImageRequest(
    val url: String,
    val sortOrder: Int,
)

data class PublishNoteRequest(
    val title: String?,
    val content: String?,
    val images: List<NoteImageRequest>,
)

data class NoteImageData(
    val id: Long,
    val url: String,
    val sortOrder: Int,
)

data class NoteData(
    val id: Long,
    val userId: Long,
    val title: String?,
    val content: String?,
    val status: Int,
    val images: List<NoteImageData>,
    val createTime: String,
    val updateTime: String,
)
