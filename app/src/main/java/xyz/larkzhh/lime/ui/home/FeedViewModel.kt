package xyz.larkzhh.lime.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import xyz.larkzhh.lime.data.network.model.FeedItem
import xyz.larkzhh.lime.domain.repository.NoteRepository
import javax.inject.Inject

data class FeedUiState(
    val items: List<FeedItem> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasMore: Boolean = true,
    val error: String? = null,
)

/**
 * 信息流页面的 ViewModel。
 * 负责管理页面的 UI 状态、处理首次加载以及分页加载更多的业务逻辑。
 */
@HiltViewModel
class FeedViewModel @Inject constructor(
    private val noteRepository: NoteRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(FeedUiState(isLoading = true))
    val uiState: StateFlow<FeedUiState> = _uiState.asStateFlow()

    private var cursor: Long? = null// 分页游标

    init {
        loadFeed()
    }

    private fun loadFeed() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, items = emptyList(), hasMore = true) }
            cursor = null
            noteRepository.getFeed(cursor = null).fold(
                onSuccess = { response ->
                    cursor = response.nextCursor
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            items = response.items,
                            hasMore = response.hasMore,
                        )
                    }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                },
            )
        }
    }

    fun loadMore() {
        val state = _uiState.value
        if (state.isLoadingMore || !state.hasMore || state.isLoading) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true) }
            noteRepository.getFeed(cursor = cursor).fold(
                onSuccess = { response ->
                    cursor = response.nextCursor
                    _uiState.update {
                        it.copy(
                            isLoadingMore = false,
                            items = it.items + response.items,
                            hasMore = response.hasMore,
                        )
                    }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(isLoadingMore = false, error = e.message) }
                },
            )
        }
    }
}
