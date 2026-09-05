package xyz.larkzhh.lime.ui.group

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import xyz.larkzhh.lime.domain.repository.ImRepository
import javax.inject.Inject

data class CreateGroupUiState(
    val name: String = "",
    val introduction: String = "",
    val isCreating: Boolean = false,
    val error: String? = null,
    val createdGroupId: String? = null,
)

/// 创建群聊页 ViewModel
@HiltViewModel
class CreateGroupViewModel @Inject constructor(
    private val imRepository: ImRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateGroupUiState())
    val uiState: StateFlow<CreateGroupUiState> = _uiState.asStateFlow()

    fun onNameChange(value: String) = _uiState.update { it.copy(name = value, error = null) }
    fun onIntroChange(value: String) = _uiState.update { it.copy(introduction = value, error = null) }

    fun create() {
        val name = _uiState.value.name.trim()
        if (name.isBlank()) {
            _uiState.update { it.copy(error = "请输入群名称") }
            return
        }
        if (_uiState.value.isCreating) return
        viewModelScope.launch {
            _uiState.update { it.copy(isCreating = true, error = null) }
            val intro = _uiState.value.introduction.trim().takeIf { it.isNotBlank() }
            imRepository.createGroup(name, intro, emptyList())
                .onSuccess { groupId ->
                    _uiState.update { it.copy(isCreating = false, createdGroupId = groupId) }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isCreating = false, error = e.message ?: "创建失败") }
                }
        }
    }

    fun clearCreatedGroupId() = _uiState.update { it.copy(createdGroupId = null) }
}
