package xyz.larkzhh.lime.data.network.model

data class UserData(
    val id: Long,
    val email: String,
    val nickname: String,
    val handle: String,
    val bio: String?,
    val avatar: String?,
    val role: String,
)