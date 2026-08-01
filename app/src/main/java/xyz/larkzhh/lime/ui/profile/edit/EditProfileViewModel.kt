package xyz.larkzhh.lime.ui.profile.edit

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import xyz.larkzhh.lime.data.network.ApiService
import xyz.larkzhh.lime.data.network.model.UpdateProfileRequest
import xyz.larkzhh.lime.domain.repository.UserRepository
import javax.inject.Inject

data class EditFormState(
    val nickname: String = "",
    val bio: String = "",
    val gender: Int = 0,
    val birthday: String = "",
    val region: String = "",
    val avatarUrl: String? = null,
    val backgroundUrl: String? = null,
)

sealed class EditProfileUiState {
    object Loading : EditProfileUiState()
    data class Ready(
        val form: EditFormState,
        val isSaving: Boolean = false,
        val isUploading: Boolean = false, // 头像、背景图上中
        val error: String? = null,
        val done: Boolean = false,
        val uploadError: String? = null,
    ) : EditProfileUiState()
    data class Error(val message: String) : EditProfileUiState()
}

/**
 * 编辑页面 ViewModel。
 * 负责处理用户信息的加载、修改更新以及头像、背景图上传的业务逻辑。
 */
@HiltViewModel
class EditProfileViewModel @Inject constructor(
    private val apiService: ApiService,
    private val userRepository: UserRepository,
    @param:ApplicationContext private val context: Context,
) : ViewModel() {

    private val _uiState = MutableStateFlow<EditProfileUiState>(EditProfileUiState.Loading)
    val uiState: StateFlow<EditProfileUiState> = _uiState.asStateFlow()

    init {
        loadUser()
    }

    /// 加载资料（优先读取缓存，无缓存时走网络）
    private fun loadUser() {
        val cached = userRepository.userFlow.value
        if (cached != null) {
            _uiState.value = EditProfileUiState.Ready(
                form = EditFormState(
                    nickname = cached.nickname,
                    bio = cached.bio ?: "",
                    gender = cached.gender ?: 0,
                    birthday = cached.birthday ?: "",
                    region = cached.region ?: "",
                    avatarUrl = cached.avatar,
                    backgroundUrl = cached.backgroundImage,
                )
            )
            return
        }
        viewModelScope.launch {
            _uiState.value = EditProfileUiState.Loading
            try {
                val response = apiService.getMe()
                if (response.code == 200 && response.data != null) {
                    userRepository.updateUser(response.data)
                    val user = response.data
                    _uiState.value = EditProfileUiState.Ready(
                        form = EditFormState(
                            nickname = user.nickname,
                            bio = user.bio ?: "",
                            gender = user.gender ?: 0,
                            birthday = user.birthday ?: "",
                            region = user.region ?: "",
                            avatarUrl = user.avatar,
                            backgroundUrl = user.backgroundImage,
                        )
                    )
                } else {
                    _uiState.value = EditProfileUiState.Error(response.message)
                }
            } catch (e: Exception) {
                _uiState.value = EditProfileUiState.Error(e.message ?: "加载失败")
            }
        }
    }

    fun onNicknameChange(value: String) = updateForm { it.copy(nickname = value) }
    fun onBioChange(value: String) = updateForm { it.copy(bio = value) }
    fun onGenderChange(value: Int) = updateForm { it.copy(gender = value) }
    fun onBirthdayChange(value: String) = updateForm { it.copy(birthday = value) }
    fun onRegionChange(value: String) = updateForm { it.copy(region = value) }

    /// 选完图片显示本地预览，上传后更新为服务器 URL，失败时回滚并提示
    fun uploadAvatar(uri: Uri) {
        val currentState = _uiState.value as? EditProfileUiState.Ready ?: return
        val originalUrl = currentState.form.avatarUrl// 原始url
        // 本地 URI 显示预览，锁定保存按钮
        _uiState.value = currentState.copy(
            form = currentState.form.copy(avatarUrl = uri.toString()),
            isUploading = true,
        )

        viewModelScope.launch {
            try {
                val part = uriToMultipart(uri, "file")
                val response = apiService.uploadAvatar(part)
                if (response.code == 200 && response.data != null) {
                    userRepository.updateUser(response.data)
                    updateForm { it.copy(avatarUrl = response.data.avatar) }
                } else {
                    // 服务器返回非 200，回滚预览
                    updateForm { it.copy(avatarUrl = originalUrl) }
                    setUploadError("头像上传失败（${response.code}）：${response.message}")
                }
            } catch (e: Exception) {
                updateForm { it.copy(avatarUrl = originalUrl) }
                setUploadError("头像上传失败：${e.message ?: "网络错误"}")
            } finally {
                // 解除保存按钮锁定
                (_uiState.value as? EditProfileUiState.Ready)?.let {
                    _uiState.value = it.copy(isUploading = false)
                }
            }
        }
    }

    /// 选完图片立即显示本地预览，上传后更新为服务器 URL，失败时回滚并提示
    fun uploadBackground(uri: Uri) {
        val currentState = _uiState.value as? EditProfileUiState.Ready ?: return
        val originalUrl = currentState.form.backgroundUrl
        // 本地 URI 显示预览，并锁定保存按钮
        _uiState.value = currentState.copy(
            form = currentState.form.copy(backgroundUrl = uri.toString()),
            isUploading = true,
        )

        viewModelScope.launch {
            try {
                val part = uriToMultipart(uri, "file")
                val response = apiService.uploadBackground(part)
                if (response.code == 200 && response.data != null) {
                    userRepository.updateUser(response.data)
                    updateForm { it.copy(backgroundUrl = response.data.backgroundImage) }
                } else {
                    updateForm { it.copy(backgroundUrl = originalUrl) }
                    setUploadError("背景图上传失败（${response.code}）：${response.message}")
                }
            } catch (e: Exception) {
                updateForm { it.copy(backgroundUrl = originalUrl) }
                setUploadError("背景图上传失败：${e.message ?: "网络错误"}")
            } finally {
                (_uiState.value as? EditProfileUiState.Ready)?.let {
                    _uiState.value = it.copy(isUploading = false)
                }
            }
        }
    }

    fun clearUploadError() {
        val state = _uiState.value as? EditProfileUiState.Ready ?: return
        _uiState.value = state.copy(uploadError = null)
    }

    private fun setUploadError(message: String) {
        val state = _uiState.value as? EditProfileUiState.Ready ?: return
        _uiState.value = state.copy(uploadError = message)
    }

    /// 保存资料
    fun saveProfile() {
        val state = _uiState.value as? EditProfileUiState.Ready ?: return
        viewModelScope.launch {
            _uiState.value = state.copy(isSaving = true, error = null)
            try {
                val form = state.form
                val request = UpdateProfileRequest(
                    nickname = form.nickname.takeIf { it.isNotBlank() },
                    bio = form.bio,
                    gender = form.gender,
                    birthday = form.birthday.takeIf { it.isNotBlank() },
                    region = form.region,
                )
                val response = apiService.updateMe(request)
                if (response.code == 200) {
                    // 拉取完整数据更新缓存
                    userRepository.refreshUser()
                    _uiState.value = state.copy(isSaving = false, done = true)
                } else {
                    _uiState.value = state.copy(isSaving = false, error = response.message)
                }
            } catch (e: Exception) {
                _uiState.value = state.copy(isSaving = false, error = e.message ?: "保存失败")
            }
        }
    }

    private fun updateForm(transform: (EditFormState) -> EditFormState) {
        val state = _uiState.value as? EditProfileUiState.Ready ?: return
        _uiState.value = state.copy(form = transform(state.form))
    }

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
}
