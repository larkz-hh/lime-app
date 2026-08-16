package xyz.larkzhh.lime.ui.profile.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import xyz.larkzhh.lime.data.network.ApiService
import xyz.larkzhh.lime.data.network.model.UserData
import xyz.larkzhh.lime.domain.repository.UserRepository
import javax.inject.Inject

sealed class ProfileUiState {
    object Loading : ProfileUiState()
    data class Success(val user: UserData) : ProfileUiState()
    data class Error(val message: String) : ProfileUiState()
}

/**
 * 个人中心页面 ViewModel。
 * 负责处理用户信息的加载以及头像上传的业务逻辑。
 */
@HiltViewModel
class ProfileViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val userRepository: UserRepository,
    private val apiService: ApiService,
    @param:ApplicationContext private val context: Context,
) : ViewModel() {

    /// 提取路由参数中的目标用户id
    private val requestedUserId: Long? = savedStateHandle["userId"]

    /// 是否是本人页面
    private val _isSelf = MutableStateFlow(
        requestedUserId == null || userRepository.userFlow.value?.id == requestedUserId
    )
    val isSelf: StateFlow<Boolean> = _isSelf.asStateFlow()

    private val _uiState = MutableStateFlow<ProfileUiState>(
        if (requestedUserId == null)
            userRepository.userFlow.value?.let { ProfileUiState.Success(it) } ?: ProfileUiState.Loading
        else
            userRepository.getCachedUserById(requestedUserId)?.let { ProfileUiState.Success(it) }
                ?: ProfileUiState.Loading
    )
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private val _uploadError = MutableStateFlow<String?>(null)// 头像上传错误
    val uploadError: StateFlow<String?> = _uploadError.asStateFlow()

    init {
        if (requestedUserId == null) {
            viewModelScope.launch {
                userRepository.userFlow.collect { user ->
                    if (user != null) _uiState.value = ProfileUiState.Success(user)
                }
            }
            loadUser()// 首次或后台刷新时从网络拉取最新数据
        } else {
            loadUserById(requestedUserId)
        }
    }

    /// 从服务端拉取最新用户数据。已有缓存时静默刷新，首次加载显示 Loading 状态
    fun loadUser() {
        viewModelScope.launch {
            // 已有数据时静默刷新
            if (_uiState.value !is ProfileUiState.Success) {
                _uiState.value = ProfileUiState.Loading
            }
            userRepository.refreshUser().onFailure { e ->
                if (e is CancellationException) return@onFailure
                if (_uiState.value !is ProfileUiState.Success) {
                    _uiState.value = ProfileUiState.Error(e.message ?: "加载失败")
                }
            }
        }
    }

    /// 加载指定用户的公开资料
    fun loadUserById(userId: Long) {
        viewModelScope.launch {
            if (_uiState.value !is ProfileUiState.Success) {
                _uiState.value = ProfileUiState.Loading
            }
            userRepository.getUserById(userId).onSuccess { user ->
                _uiState.value = ProfileUiState.Success(user)
            }.onFailure { e ->
                if (e is CancellationException) return@onFailure
                if (_uiState.value !is ProfileUiState.Success) {
                    _uiState.value = ProfileUiState.Error(e.message ?: "加载失败")
                }
            }
        }
    }

    /// 上传用户头像。
    fun uploadAvatar(uri: Uri) {
        viewModelScope.launch {
            try {
                val part = uriToMultipart(uri, "file")
                val response = apiService.uploadAvatar(part)
                if (response.code == 200 && response.data != null) {
                    userRepository.updateUser(response.data)
                } else {
                    _uploadError.value = "头像上传失败（${response.code}）：${response.message}"
                }
            } catch (e: Exception) {
                _uploadError.value = "头像上传失败：${e.message ?: "网络错误"}"
            }
        }
    }

    fun clearUploadError() { _uploadError.value = null }

    ///  将本地图片的 Uri 转换为 Retrofit 支持的 MultipartBody.Part 对象
    /// 读取图片字节流，识别 MIME 类型并生成对应的文件名。
    private fun uriToMultipart(uri: Uri, partName: String): MultipartBody.Part {
        val bytes = context.contentResolver.openInputStream(uri)?.readBytes()
            ?: throw IllegalArgumentException("无法读取图片")
        // 从 ContentResolver 取 MIME 类型
        val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
        // 文件后缀名批评
        val ext = when (mimeType) {
            "image/png" -> "png"
            "image/webp" -> "webp"
            "image/gif" -> "gif"
            else -> "jpg"
        }
        val body = bytes.toRequestBody(mimeType.toMediaType())// 将字节流和 MIME 类型打包为 RequestBody
        return MultipartBody.Part.createFormData(partName, "upload.$ext", body)
    }
}