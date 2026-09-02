package xyz.larkzhh.lime.domain.repository

import android.net.Uri
import kotlinx.coroutines.flow.StateFlow
import xyz.larkzhh.lime.data.network.model.UserData

/**
 * 用户数据仓库接口
 */
interface UserRepository {
    /// 当前用户数据，null：未登录或尚未加载
    val userFlow: StateFlow<UserData?>

    /// 更新内存状态和本地缓存
    fun updateUser(user: UserData)

    /// 从服务端拉取最新数据，成功后写入缓存
    suspend fun refreshUser(): Result<UserData>

    /// 获取指定用户的公开资料
    suspend fun getUserById(userId: Long): Result<UserData>

    /// 同步读取指定用户已缓存的资料
    fun getCachedUserById(userId: Long): UserData?

    /// 上传头像，本地缓存
    suspend fun uploadAvatar(uri: Uri): Result<UserData>

    /// 上传背景图，本地缓存
    suspend fun uploadBackground(uri: Uri): Result<UserData>

    /// 更新个人资料，本地缓存
    suspend fun updateProfile(
        nickname: String?,
        bio: String?,
        gender: Int,
        birthday: String?,
        region: String?,
    ): Result<UserData>

    /// 注销时清除本地缓存
    fun clearUser()
}
