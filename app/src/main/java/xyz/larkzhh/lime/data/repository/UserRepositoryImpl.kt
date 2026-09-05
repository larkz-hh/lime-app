package xyz.larkzhh.lime.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.gson.Gson
import com.tencent.mmkv.MMKV
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import xyz.larkzhh.lime.data.im.ImManager
import xyz.larkzhh.lime.data.network.ApiService
import xyz.larkzhh.lime.data.network.model.UpdateProfileRequest
import xyz.larkzhh.lime.data.network.model.UserData
import xyz.larkzhh.lime.domain.repository.UserRepository
import xyz.larkzhh.lime.util.LruCache
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 用户数据仓库实现
 */
@Singleton
class UserRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
    @param:ApplicationContext private val context: Context,
    private val imManager: ImManager,
) : UserRepository {

    private val mmkv by lazy { MMKV.defaultMMKV() }
    private val gson = Gson()

    private val userByIdCache = LruCache<Long, UserData>(maxSize = 50)

    private val _userFlow = MutableStateFlow<UserData?>(loadFromCache())
    override val userFlow: StateFlow<UserData?> = _userFlow.asStateFlow()

    /// 加载缓存
    private fun loadFromCache(): UserData? {
        val json = mmkv.decodeString(KEY_USER) ?: return null
        return runCatching { gson.fromJson(json, UserData::class.java) }.getOrNull()
    }

    /// 更新用户数据
    override fun updateUser(user: UserData) {
        _userFlow.value = user
        mmkv.encode(KEY_USER, gson.toJson(user))
    }

    /// 网络刷新
    override suspend fun refreshUser(): Result<UserData> = runCatching {
        val response = apiService.getMe()
        check(response.code == 200 && response.data != null) { response.message }
        updateUser(response.data)
        response.data
    }

    /// 获取指定用户公开资料
    override suspend fun getUserById(userId: Long): Result<UserData> = runCatching {
        val response = apiService.getUserById(userId)
        check(response.code == 200 && response.data != null) { response.message }
        userByIdCache[userId] = response.data
        response.data
    }

    /// 获取指定用户公开资料
    override suspend fun getUserByHandle(handle: String): Result<UserData> = runCatching {
        val response = apiService.getUserByHandle(handle)
        check(response.code == 200 && response.data != null) { response.message }
        userByIdCache[response.data.id] = response.data
        response.data
    }

    /// 获取指定用户公开资料
    override suspend fun getUserByUid(uid: String): Result<UserData> = runCatching {
        val response = apiService.getUserByUid(uid)
        check(response.code == 200 && response.data != null) { response.message }
        userByIdCache[response.data.id] = response.data
        response.data
    }

    /// 同步读取指定用户已缓存的信息
    override fun getCachedUserById(userId: Long): UserData? = userByIdCache[userId]

    /// 上传头像。本地缓存
    override suspend fun uploadAvatar(uri: Uri): Result<UserData> {
        val part = runCatching { uriToMultipart(uri, "file") }
            .getOrElse { return Result.failure(it) }
        return runCatching {
            val response = apiService.uploadAvatar(part)
            check(response.code == 200 && response.data != null) { response.message }
            updateUser(response.data)
            syncImProfile(response.data)
            response.data
        }
    }

    /// 互关好友列表
    override suspend fun getMutualFriends(): Result<List<UserData>> = runCatching {
        val response = apiService.getMutualFriends()
        check(response.code == 200 && response.data != null) { response.message }
        response.data
    }

    /// 上传群头像
    override suspend fun uploadGroupAvatar(uri: Uri): Result<String> {
        val part = runCatching { uriToMultipart(uri, "file") }
            .getOrElse { return Result.failure(it) }
        return runCatching {
            val response = apiService.uploadGroupAvatar(part)
            check(response.code == 200 && response.data != null) { response.message }
            response.data
        }
    }

    /// 上传背景图，本地缓存
    override suspend fun uploadBackground(uri: Uri): Result<UserData> {
        val part = runCatching { uriToMultipart(uri, "file") }
            .getOrElse { return Result.failure(it) }
        return runCatching {
            val response = apiService.uploadBackground(part)
            check(response.code == 200 && response.data != null) { response.message }
            updateUser(response.data)
            response.data
        }
    }

    /// 更新个人资，本地缓存
    override suspend fun updateProfile(
        nickname: String?,
        bio: String?,
        gender: Int,
        birthday: String?,
        region: String?,
    ): Result<UserData> = runCatching {
        val request = UpdateProfileRequest(nickname, bio, gender, birthday, region)
        val response = apiService.updateMe(request)
        check(response.code == 200 && response.data != null) { response.message }
        updateUser(response.data)
        syncImProfile(response.data)
        response.data
    }

    /// 清空用户数据
    override fun clearUser() {
        _userFlow.value = null
        mmkv.removeValueForKey(KEY_USER)
    }

    /// 资料变更后同步昵称、头像到 IM
    private suspend fun syncImProfile(user: UserData) {
        if (imManager.getLoginUser() == null) return
        runCatching {
            imManager.updateSelfProfile(user.nickname, user.avatar)
        }.onFailure { e ->
            Log.w(TAG, "syncImProfile failed", e)
        }
    }

    /// 读取图片字节并组装 MultipartBody.Part
    private fun uriToMultipart(uri: Uri, partName: String): MultipartBody.Part {
        val bytes = context.contentResolver.openInputStream(uri)?.readBytes()
            ?: throw IllegalArgumentException("无法读取图片")
        val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
        val ext = when (mimeType) {
            "image/png" -> "png"
            "image/webp" -> "webp"
            "image/gif" -> "gif"
            else -> "jpg"
        }
        val body = bytes.toRequestBody(mimeType.toMediaType())
        return MultipartBody.Part.createFormData(partName, "upload.$ext", body)
    }

    private companion object {
        const val KEY_USER = "cached_user_data"
        const val TAG = "UserRepositoryImpl"
    }
}
