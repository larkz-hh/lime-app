package xyz.larkzhh.lime.ui.draft

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import xyz.larkzhh.lime.data.network.model.FeedItem
import xyz.larkzhh.lime.domain.repository.NoteRepository
import xyz.larkzhh.lime.domain.repository.UserRepository
import xyz.larkzhh.lime.util.cache.JsonListCache
import javax.inject.Inject

data class DraftBoxUiState(
    val items: List<FeedItem> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasMore: Boolean = true,
    val error: String? = null,
    // 管理模式
    val isManaging: Boolean = false,
    val selectedIds: Set<Long> = emptySet(),
    val isDeleting: Boolean = false,
    val deleteError: String? = null,
)

/**
 * 草稿箱 ViewModel
 */
@HiltViewModel
class DraftBoxViewModel @Inject constructor(
    private val noteRepository: NoteRepository,
    userRepository: UserRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DraftBoxUiState())
    val uiState: StateFlow<DraftBoxUiState> = _uiState.asStateFlow()

    private var selfUserId: Long? = null
    private var cursor: Long? = null
    private val pageSize = 20

    init {
        // 等当前用户 id 就绪后加载草稿
        viewModelScope.launch {
            selfUserId = userRepository.userFlow.filterNotNull().first().id
            load()
        }
    }

    private fun cacheKey(userId: Long) = "draft_box_v1_$userId"

    /// 加载
    private fun load() {
        val userId = selfUserId ?: return
        viewModelScope.launch {
            cursor = null
            val cached = JsonListCache.read<FeedItem>(
                cacheKey(userId),
                object : TypeToken<List<FeedItem>>() {}.type,
            )
            _uiState.update { state ->
                if (cached.isNullOrEmpty()) {
                    state.copy(isLoading = true, error = null, items = emptyList(), hasMore = true)
                } else {
                    state.copy(isLoading = false, error = null, items = cached, hasMore = false)
                }
            }
            // 网络后台刷新
            noteRepository.fetchUserNotes(userId, cursor = null, size = pageSize, status = "draft")
                .fold(
                    onSuccess = { response ->
                        cursor = response.nextCursor
                        // 缓存
                        JsonListCache.save(cacheKey(userId), response.items)
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                items = response.items,
                                hasMore = response.hasMore,
                            )
                        }
                    },
                    onFailure = {
                        _uiState.update { state ->
                            if (state.items.isEmpty()) state.copy(isLoading = false, error = it.message)
                            else state
                        }
                    },
                )
        }
    }

    /// 下拉刷新
    fun refresh() = load()

    /// 加载更多
    fun loadMore() {
        val state = _uiState.value
        if (state.isLoadingMore || !state.hasMore || state.isLoading) return
        val userId = selfUserId ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true) }
            noteRepository.fetchUserNotes(userId, cursor, pageSize, status = "draft")
                .fold(
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

    /// 进入管理模式
    fun enterManageMode() {
        _uiState.update { it.copy(isManaging = true, selectedIds = emptySet()) }
    }

    /// 退出管理模式
    fun exitManageMode() {
        _uiState.update { it.copy(isManaging = false, selectedIds = emptySet()) }
    }

    /// 勾选、取消勾选
    fun toggleSelect(id: Long) {
        _uiState.update { state ->
            val newIds = if (id in state.selectedIds) state.selectedIds - id else state.selectedIds + id
            state.copy(selectedIds = newIds)
        }
    }

    /// 删除勾选的草稿
    fun deleteSelected() {
        val ids = _uiState.value.selectedIds.toList()
        if (ids.isEmpty() || _uiState.value.isDeleting) return
        viewModelScope.launch {
            _uiState.update { it.copy(isDeleting = true, deleteError = null) }
            var firstError: Throwable? = null
            ids.forEach { id ->
                noteRepository.deleteNote(id).onFailure { e ->
                    if (firstError == null) firstError = e
                }
            }
            _uiState.update { s ->
                val allOk = firstError == null
                s.copy(
                    isDeleting = false,
                    items = if (allOk) s.items.filter { it.id !in ids } else s.items,
                    selectedIds = if (allOk) emptySet() else s.selectedIds - ids,
                    isManaging = if (allOk) false else s.isManaging,
                    deleteError = firstError?.message,
                )
            }
        }
    }

    fun clearDeleteError() {
        _uiState.update { it.copy(deleteError = null) }
    }
}
