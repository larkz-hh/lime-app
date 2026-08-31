package xyz.larkzhh.lime.ui.ai

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
import xyz.larkzhh.lime.domain.repository.UserRepository
import javax.inject.Inject

enum class NotePickerTab(val label: String) { FAVORITES("收藏"), LIKES("点赞"), PUBLISHED("发布") }

data class NotePickerTabState(
    val items: List<FeedItem> = emptyList(),
    val isLoading: Boolean = false,
    val hasMore: Boolean = true,
    val error: String? = null,
)

/**
 * 笔记选择器 ViewModel
 */
@HiltViewModel
class NotePickerViewModel @Inject constructor(
    private val noteRepository: NoteRepository,
    private val userRepository: UserRepository,
) : ViewModel() {

    private val _states = MutableStateFlow(
        mapOf(
            NotePickerTab.FAVORITES to NotePickerTabState(),
            NotePickerTab.LIKES to NotePickerTabState(),
            NotePickerTab.PUBLISHED to NotePickerTabState(),
        )
    )
    val states: StateFlow<Map<NotePickerTab, NotePickerTabState>> = _states.asStateFlow()

    private val cursors = mutableMapOf<NotePickerTab, Long?>()
    private val loadedTabs = mutableSetOf<NotePickerTab>()

    /// 加载
    fun load(tab: NotePickerTab) {
        if (loadedTabs.contains(tab)) return
        loadMore(tab, refresh = true)
    }

    fun loadMore(tab: NotePickerTab, refresh: Boolean = false) {
        val state = _states.value[tab] ?: NotePickerTabState()
        if (state.isLoading) return
        if (!refresh && !state.hasMore) return
        val userId = userRepository.userFlow.value?.id ?: return
        _states.update { m -> m + (tab to state.copy(isLoading = true, error = null)) }
        viewModelScope.launch {
            val result = when (tab) {
                NotePickerTab.FAVORITES -> noteRepository.getUserFavorites(userId, if (refresh) null else cursors[tab])
                NotePickerTab.LIKES -> noteRepository.getUserLikes(userId, if (refresh) null else cursors[tab])
                NotePickerTab.PUBLISHED -> noteRepository.getUserNotes(userId, if (refresh) null else cursors[tab])
            }
            result.fold(
                onSuccess = { resp ->
                    val items = resp.items.filter { it.noteType == 1 }// 只留图文
                    cursors[tab] = resp.nextCursor
                    loadedTabs += tab
                    _states.update { m ->
                        val old = m[tab] ?: NotePickerTabState()
                        m + (tab to old.copy(
                            items = if (refresh) items else old.items + items,
                            isLoading = false,
                            hasMore = resp.hasMore,
                        ))
                    }
                },
                onFailure = { e ->
                    _states.update { m ->
                        m + (tab to (m[tab] ?: NotePickerTabState()).copy(isLoading = false, error = e.message))
                    }
                },
            )
        }
    }
}
