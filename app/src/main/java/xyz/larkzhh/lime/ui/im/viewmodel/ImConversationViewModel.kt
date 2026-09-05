package xyz.larkzhh.lime.ui.im.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import xyz.larkzhh.lime.domain.model.ImConversation
import xyz.larkzhh.lime.domain.repository.ImRepository
import javax.inject.Inject

data class ImConversationUiState(
    val conversations: List<ImConversation> = emptyList(),
    val isLoading: Boolean = false,
)

/**
 * 私信会话列表 ViewModel
 */
@HiltViewModel
class ImConversationViewModel @Inject constructor(
    private val imRepository: ImRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ImConversationUiState())
    val state: StateFlow<ImConversationUiState> = _state.asStateFlow()

    init {
        refresh()
        // 会话变化重新拉取
        viewModelScope.launch {
            imRepository.conversationChanges.collect {
                val list = imRepository.getConversations()
                _state.update { it.copy(conversations = list) }
            }
        }
    }

    /// 确保登录后拉取会话列表
    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            imRepository.ensureImLogin().fold(
                onSuccess = {
                    val list = imRepository.getConversations()
                    _state.update { it.copy(isLoading = false, conversations = list) }
                },
                onFailure = {
                    _state.update { it.copy(isLoading = false) }
                },
            )
        }
    }

    /// 删除会话。清空聊天记录
    fun deleteConversation(conversationId: String) {
        viewModelScope.launch {
            imRepository.clearHistory(conversationId)
            imRepository.deleteConversation(conversationId)
            _state.update {
                it.copy(conversations = it.conversations.filterNot { c -> c.conversationId == conversationId })
            }
        }
    }
}
