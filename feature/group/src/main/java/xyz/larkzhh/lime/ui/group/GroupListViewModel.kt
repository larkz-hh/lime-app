package xyz.larkzhh.lime.ui.group

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import xyz.larkzhh.lime.domain.model.ImGroup
import xyz.larkzhh.lime.domain.repository.ImRepository
import javax.inject.Inject

data class GroupListUiState(
    val isLoading: Boolean = true,
    val groups: List<ImGroup> = emptyList(),
    val selfImUserId: String? = null,
)

/// 群聊列表页 ViewModel
@HiltViewModel
class GroupListViewModel @Inject constructor(
    private val imRepository: ImRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(GroupListUiState())
    val uiState: StateFlow<GroupListUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            imRepository.ensureImLogin().getOrNull()
            val groups = imRepository.getJoinedGroups()
            _uiState.update {
                it.copy(isLoading = false, groups = groups, selfImUserId = imRepository.getLoginUser())
            }
        }
    }
}
