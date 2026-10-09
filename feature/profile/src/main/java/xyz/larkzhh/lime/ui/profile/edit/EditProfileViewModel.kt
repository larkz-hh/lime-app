package xyz.larkzhh.lime.ui.profile.edit

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import xyz.larkzhh.lime.feature.profile.R
import xyz.larkzhh.lime.data.network.model.UserData
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
 * 负责用户信息的加载、修改更新以及头像、背景图上传。
 */
@HiltViewModel
class EditProfileViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userRepository: UserRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<EditProfileUiState>(EditProfileUiState.Loading)
    val uiState: StateFlow<EditProfileUiState> = _uiState.asStateFlow()

    init {
        loadUser()
    }

    /// 加载资料
    private fun loadUser() {
        val cached = userRepository.userFlow.value
        if (cached != null) {
            _uiState.value = EditProfileUiState.Ready(form = cached.toEditForm())
            return
        }
        viewModelScope.launch {
            _uiState.value = EditProfileUiState.Loading
            userRepository.refreshUser()
                .onSuccess { user -> _uiState.value = EditProfileUiState.Ready(form = user.toEditForm()) }
                .onFailure { e ->
                    if (e is CancellationException) return@onFailure
                    _uiState.value = EditProfileUiState.Error(e.message ?: context.getString(R.string.edit_load_failed))
                }
        }
    }

    fun onNicknameChange(value: String) = updateForm { it.copy(nickname = value) }
    fun onBioChange(value: String) = updateForm { it.copy(bio = value) }
    fun onGenderChange(value: Int) = updateForm { it.copy(gender = value) }
    fun onBirthdayChange(value: String) = updateForm { it.copy(birthday = value) }
    fun onRegionChange(value: String) = updateForm { it.copy(region = value) }

    /// 上传头像
    fun uploadAvatar(uri: Uri) {
        val currentState = _uiState.value as? EditProfileUiState.Ready ?: return
        val originalUrl = currentState.form.avatarUrl// 原始url
        _uiState.value = currentState.copy(
            form = currentState.form.copy(avatarUrl = uri.toString()),
            isUploading = true,
        )
        viewModelScope.launch {
            try {
                val user = userRepository.uploadAvatar(uri).getOrThrow()
                updateForm { it.copy(avatarUrl = user.avatar) }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                updateForm { it.copy(avatarUrl = originalUrl) }
                val reason = e.message ?: context.getString(R.string.edit_network_error)
                setUploadError(context.getString(R.string.edit_avatar_upload_failed, reason))
            } finally {
                (_uiState.value as? EditProfileUiState.Ready)?.let {
                    _uiState.value = it.copy(isUploading = false)
                }
            }
        }
    }

    /// 上传背景图
    fun uploadBackground(uri: Uri) {
        val currentState = _uiState.value as? EditProfileUiState.Ready ?: return
        val originalUrl = currentState.form.backgroundUrl
        _uiState.value = currentState.copy(
            form = currentState.form.copy(backgroundUrl = uri.toString()),
            isUploading = true,
        )
        viewModelScope.launch {
            try {
                val user = userRepository.uploadBackground(uri).getOrThrow()
                updateForm { it.copy(backgroundUrl = user.backgroundImage) }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                updateForm { it.copy(backgroundUrl = originalUrl) }
                val reason = e.message ?: context.getString(R.string.edit_network_error)
                setUploadError(context.getString(R.string.edit_bg_upload_failed, reason))
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
            val form = state.form
            userRepository.updateProfile(
                nickname = form.nickname.takeIf { it.isNotBlank() },
                bio = form.bio,
                gender = form.gender,
                birthday = form.birthday.takeIf { it.isNotBlank() },
                region = form.region,
            ).onSuccess {
                _uiState.value = state.copy(isSaving = false, done = true)
            }.onFailure { e ->
                if (e is CancellationException) return@onFailure
                _uiState.value = state.copy(isSaving = false, error = e.message ?: context.getString(R.string.edit_save_failed))
            }
        }
    }

    private fun updateForm(transform: (EditFormState) -> EditFormState) {
        val state = _uiState.value as? EditProfileUiState.Ready ?: return
        _uiState.value = state.copy(form = transform(state.form))
    }

    private fun UserData.toEditForm() = EditFormState(
        nickname = nickname,
        bio = bio ?: "",
        gender = gender ?: 0,
        birthday = birthday ?: "",
        region = region ?: "",
        avatarUrl = avatar,
        backgroundUrl = backgroundImage,
    )
}
