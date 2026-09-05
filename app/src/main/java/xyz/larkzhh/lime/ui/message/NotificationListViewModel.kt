package xyz.larkzhh.lime.ui.message

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import xyz.larkzhh.lime.data.network.model.NotificationData
import xyz.larkzhh.lime.domain.model.FollowRelation
import xyz.larkzhh.lime.domain.model.NotificationCategory
import xyz.larkzhh.lime.domain.model.NotificationType
import xyz.larkzhh.lime.domain.model.typeParam
import xyz.larkzhh.lime.domain.repository.FollowRepository
import xyz.larkzhh.lime.domain.repository.NotificationRepository
import xyz.larkzhh.lime.navigation.Screen
import javax.inject.Inject

data class NotificationPageState(
    val isInitialLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasMore: Boolean = true,
    val error: String? = null,
    val loaded: Boolean = false,
)

/**
 * 通知列表页 ViewModel
 */
@HiltViewModel
class NotificationListViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: NotificationRepository,
    private val followRepository: FollowRepository,
) : ViewModel() {

    val category: NotificationCategory = NotificationCategory.fromRouteKey(
        savedStateHandle.get<String>(Screen.NotificationList.ARG_TYPE),
    )

    private val _pageState = MutableStateFlow(NotificationPageState())
    val pageState: StateFlow<NotificationPageState> = _pageState.asStateFlow()

    // 当前分类的分页列表
    private val _items = MutableStateFlow<List<NotificationData>>(emptyList())
    val items: StateFlow<List<NotificationData>> = _items.asStateFlow()

    val relations = followRepository.relations

    private var cursor: Long? = null

    init {
        loadInitial()
    }

    /// 初始加载
    fun loadInitial() {
        val state = _pageState.value
        if (state.loaded || state.isInitialLoading) return
        _pageState.update { it.copy(isInitialLoading = true, error = null) }
        viewModelScope.launch {
            repository.loadPage(cursor = null, size = 50, type = category.typeParam).fold(
                onSuccess = { response ->
                    cursor = response.nextCursor?.toLongOrNull()
                    _items.value = response.items
                    _pageState.update {
                        it.copy(isInitialLoading = false, hasMore = response.hasMore, loaded = true)
                    }
                    seedFollowRelations(response.items)
                    // 打开列表标记为已读
                    repository.markAllRead(category)
                },
                onFailure = { e ->
                    _pageState.update { it.copy(isInitialLoading = false, error = e.message) }
                },
            )
        }
    }

    /// 加载更多
    fun loadMore() {
        val state = _pageState.value
        if (state.isLoadingMore || state.isInitialLoading || !state.hasMore) return
        _pageState.update { it.copy(isLoadingMore = true) }
        viewModelScope.launch {
            repository.loadPage(cursor, 20, category.typeParam).fold(
                onSuccess = { response ->
                    cursor = response.nextCursor?.toLongOrNull()
                    _items.value = (_items.value + response.items).distinctBy { it.id }
                    _pageState.update { it.copy(isLoadingMore = false, hasMore = response.hasMore) }
                    seedFollowRelations(response.items)
                },
                onFailure = {
                    _pageState.update { it.copy(isLoadingMore = false) }
                },
            )
        }
    }

    /// 标记已读
    fun markRead(item: NotificationData) {
        viewModelScope.launch { repository.markRead(item.id) }
    }

    /// 删除
    fun delete(item: NotificationData) {
        _items.update { list -> list.filterNot { it.id == item.id } }
        viewModelScope.launch { repository.delete(item.id) }
    }

    /// 关注
    fun follow(userId: Long) {
        viewModelScope.launch { followRepository.follow(userId) }
    }

    /// 取消关注
    fun unfollow(userId: Long) {
        viewModelScope.launch { followRepository.unfollow(userId) }
    }

    /// 初始关注关系
    private fun seedFollowRelations(items: List<NotificationData>) {
        items
            .filter { it.type == NotificationType.Follow.code && it.senderId != null }
            .forEach { item ->
                val id = item.senderId ?: return@forEach
                if (relations.value[id] == null) {
                    followRepository.updateRelation(
                        id,
                        FollowRelation(
                            following = item.isFollowing ?: false,
                            followedBack = item.isFollowedBack ?: false,
                        ),
                    )
                }
            }
    }
}
