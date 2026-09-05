package xyz.larkzhh.lime.ui.group

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
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
    private val imRepository: ImRepository,
    private val userRepository: UserRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(GroupManageUiState())
    val uiState: StateFlow<GroupManageUiState> = _uiState.asStateFlow()

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
            }.onFailure { e ->
                _uiState.update { it.copy(isSaving = false, error = e.message ?: "保存失败") }
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
                    .onSuccess { reloadGroup(groupId) }
                    .onFailure { e -> _uiState.update { it.copy(isSaving = false, error = e.message ?: "头像保存失败") } }
            }.onFailure { e ->
                _uiState.update { it.copy(isSaving = false, error = e.message ?: "头像上传失败") }
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
                .onFailure { e -> _uiState.update { it.copy(isSaving = false, error = e.message ?: "邀请失败") } }
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
                .onFailure { e -> _uiState.update { it.copy(isSaving = false, error = e.message ?: "解散失败") } }
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
                .onFailure { e -> _uiState.update { it.copy(isSaving = false, error = e.message ?: "退出失败") } }
        }
    }

    /// 重新拉取群资料
    private suspend fun reloadGroup(groupId: String) {
        val group = imRepository.getGroupsInfo(listOf(groupId)).firstOrNull()
        _uiState.update { it.copy(isSaving = false, group = group) }
    }
}
