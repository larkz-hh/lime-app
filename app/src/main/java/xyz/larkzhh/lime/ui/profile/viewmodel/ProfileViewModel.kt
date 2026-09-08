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
import xyz.larkzhh.lime.R
import xyz.larkzhh.lime.data.network.model.UserData
import xyz.larkzhh.lime.domain.model.FollowRelation
import xyz.larkzhh.lime.domain.repository.FollowRepository
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
    @ApplicationContext private val context: Context,
    savedStateHandle: SavedStateHandle,
    private val userRepository: UserRepository,
    private val followRepository: FollowRepository,
) : ViewModel() {

    /// 提取路由参数中的目标用户id
    private val requestedUserId: Long? = savedStateHandle["userId"]

    /// 是否是本人页面
    private val _isSelf = MutableStateFlow(
        requestedUserId == null || userRepository.userFlow.value?.id == requestedUserId
    )
    val isSelf: StateFlow<Boolean> = _isSelf.asStateFlow()

    private val _uiState = MutableStateFlow(
        if (requestedUserId == null)
            userRepository.userFlow.value?.let { ProfileUiState.Success(it) } ?: ProfileUiState.Loading
        else
            userRepository.getCachedUserById(requestedUserId)?.let { ProfileUiState.Success(it) }
                ?: ProfileUiState.Loading
    )
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private val _uploadError = MutableStateFlow<String?>(null)// 头像上传错误
    val uploadError: StateFlow<String?> = _uploadError.asStateFlow()

    private val _followError = MutableStateFlow<String?>(null)// 关注操作错误
    val followError: StateFlow<String?> = _followError.asStateFlow()

    /// 共享关注关系
    val relations = followRepository.relations

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
                    _uiState.value = ProfileUiState.Error(e.message ?: context.getString(R.string.profile_load_failed))
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
                seedRelation(user)
            }.onFailure { e ->
                if (e is CancellationException) return@onFailure
                if (_uiState.value !is ProfileUiState.Success) {
                    _uiState.value = ProfileUiState.Error(e.message ?: context.getString(R.string.profile_load_failed))
                }
            }
        }
    }

    /// 上传用户头像
    fun uploadAvatar(uri: Uri) {
        viewModelScope.launch {
            userRepository.uploadAvatar(uri)
                .onFailure { e ->
                    if (e is CancellationException) return@onFailure
                    val reason = e.message ?: context.getString(R.string.profile_network_error)
                    _uploadError.value = context.getString(R.string.profile_avatar_upload_failed, reason)
                }
        }
    }

    fun clearUploadError() { _uploadError.value = null }

    fun clearFollowError() { _followError.value = null }

    /// 关注当前查看的用户
    fun follow() {
        val user = (_uiState.value as? ProfileUiState.Success)?.user ?: return
        val selfId = userRepository.userFlow.value?.id
        if (selfId == null || user.id == selfId) return
        viewModelScope.launch {
            followRepository.follow(user.id)
                .onSuccess {
                    updateFollowState(user.id, following = true)
                    loadUserById(user.id)
                }
                .onFailure { e ->
                    if (e is CancellationException) return@onFailure
                    val reason = e.message ?: context.getString(R.string.profile_network_error)
                    _followError.value = context.getString(R.string.profile_follow_failed, reason)
                }
        }
    }

    /// 取消关注当前查看的用户
    fun unfollow() {
        val user = (_uiState.value as? ProfileUiState.Success)?.user ?: return
        val selfId = userRepository.userFlow.value?.id
        if (selfId == null || user.id == selfId) return
        viewModelScope.launch {
            followRepository.unfollow(user.id)
                .onSuccess {
                    updateFollowState(user.id, following = false)
                    loadUserById(user.id)
                }
                .onFailure { e ->
                    if (e is CancellationException) return@onFailure
                    val reason = e.message ?: context.getString(R.string.profile_network_error)
                    _followError.value = context.getString(R.string.profile_unfollow_failed, reason)
                }
        }
    }

    /// 乐观更新粉丝数与关注状态
    private fun updateFollowState(userId: Long, following: Boolean) {
        val current = (_uiState.value as? ProfileUiState.Success)?.user ?: return
        if (current.id != userId) return
        val followerCount = current.followerCount?.let { if (following) it + 1 else it - 1 }
        _uiState.value = ProfileUiState.Success(
            current.copy(isFollowing = following, followerCount = followerCount)
        )
    }

    /// 写入共享关系
    private fun seedRelation(user: UserData) {
        val selfId = userRepository.userFlow.value?.id
        if (selfId != null && user.id != selfId) {
            followRepository.updateRelation(
                user.id,
                FollowRelation(user.isFollowing ?: false, user.isFollowedBack ?: false),
            )
        }
    }
}
