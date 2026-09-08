package xyz.larkzhh.lime.ui.group

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import xyz.larkzhh.lime.R
import xyz.larkzhh.lime.data.network.model.UserData
import xyz.larkzhh.lime.domain.model.ImGroup
import xyz.larkzhh.lime.domain.repository.ImRepository
import xyz.larkzhh.lime.domain.repository.UserRepository
import javax.inject.Inject

data class GroupManageUiState(
    val groupId: String = "",
    val group: ImGroup? = null,
    val selfRole: Int = 200,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val error: String? = null,
    val mutualFriends: List<UserData> = emptyList(),
    val done: Boolean = false,// 解散或退出完成
) {
    val isOwner: Boolean get() = selfRole == 400
}

/// 群管理页 ViewModel
@HiltViewModel
class GroupManageViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val imRepository: ImRepository,
    private val userRepository: UserRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(GroupManageUiState())
    val uiState: StateFlow<GroupManageUiState> = _uiState.asStateFlow()

    /// 保存成功
    private val _saved = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val saved: SharedFlow<Unit> = _saved.asSharedFlow()

    /// 拉取群资料、当前角色、互关好友列表
    fun load(groupId: String) {
        if (_uiState.value.groupId == groupId) return
        _uiState.update { it.copy(groupId = groupId, isLoading = true) }
        viewModelScope.launch {
            imRepository.ensureImLogin().getOrNull()
            val groups = imRepository.getGroupsInfo(listOf(groupId))
            val role = imRepository.getSelfRole(groupId).getOrDefault(200)
            val friends = userRepository.getMutualFriends().getOrDefault(emptyList())
            _uiState.update {
                it.copy(
                    isLoading = false,
                    group = groups.firstOrNull(),
                    selfRole = role,
                    mutualFriends = friends,
                )
            }
        }
    }

    /// 保存群名称或简介
    fun saveInfo(name: String, introduction: String) {
        val groupId = _uiState.value.groupId
        if (groupId.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
            imRepository.updateGroupInfo(
                groupId = groupId,
                name = name.trim().takeIf { it.isNotBlank() },
                introduction = introduction.trim().takeIf { it.isNotBlank() },
            ).onSuccess {
                reloadGroup(groupId)
                _saved.tryEmit(Unit)
            }.onFailure { e ->
                _uiState.update { it.copy(isSaving = false, error = e.message ?: context.getString(R.string.group_save_failed)) }
            }
        }
    }

    /// 上传群头像
    fun uploadAvatar(uri: Uri) {
        val groupId = _uiState.value.groupId
        if (groupId.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
            userRepository.uploadGroupAvatar(uri).onSuccess { url ->
                imRepository.updateGroupInfo(groupId, faceUrl = url)
                    .onSuccess {
                        reloadGroup(groupId)
                        _saved.tryEmit(Unit)
                    }
                    .onFailure { e -> _uiState.update { it.copy(isSaving = false, error = e.message ?: context.getString(R.string.group_avatar_save_failed)) } }
            }.onFailure { e ->
                _uiState.update { it.copy(isSaving = false, error = e.message ?: context.getString(R.string.group_avatar_upload_failed)) }
            }
        }
    }

    /// 邀请互关好友入群
    fun invite(users: List<UserData>) {
        val groupId = _uiState.value.groupId
        if (groupId.isBlank() || users.isEmpty()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
            val ids = users.map { "lime_${it.id}" }
            imRepository.inviteToGroup(groupId, ids)
                .onSuccess { reloadGroup(groupId) }
                .onFailure { e -> _uiState.update { it.copy(isSaving = false, error = e.message ?: context.getString(R.string.group_invite_failed)) } }
        }
    }

    /// 解散群聊
    fun dismissGroup() {
        val groupId = _uiState.value.groupId
        if (groupId.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
            imRepository.dismissGroup(groupId)
                .onSuccess { _uiState.update { it.copy(isSaving = false, done = true) } }
                .onFailure { e -> _uiState.update { it.copy(isSaving = false, error = e.message ?: context.getString(R.string.group_dismiss_failed)) } }
        }
    }

    /// 退出群聊
    fun quitGroup() {
        val groupId = _uiState.value.groupId
        if (groupId.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
            imRepository.quitGroup(groupId)
                .onSuccess { _uiState.update { it.copy(isSaving = false, done = true) } }
                .onFailure { e -> _uiState.update { it.copy(isSaving = false, error = e.message ?: context.getString(R.string.group_quit_failed)) } }
        }
    }

    /// 重新拉取群资料
    private suspend fun reloadGroup(groupId: String) {
        val group = imRepository.getGroupsInfo(listOf(groupId)).firstOrNull()
        _uiState.update { it.copy(isSaving = false, group = group) }
    }
}
