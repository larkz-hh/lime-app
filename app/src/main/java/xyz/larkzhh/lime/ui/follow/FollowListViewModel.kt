package xyz.larkzhh.lime.ui.follow

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import xyz.larkzhh.lime.data.network.model.FollowListItem
import xyz.larkzhh.lime.data.network.model.FollowListResponse
import xyz.larkzhh.lime.domain.model.FollowRelation
import xyz.larkzhh.lime.domain.repository.FollowRepository
import xyz.larkzhh.lime.navigation.route.Screen
import javax.inject.Inject

/// 关注、粉丝 tab
enum class FollowTab(val routeValue: String) {
    Following(Screen.FollowList.TAB_FOLLOWING),
    Followers(Screen.FollowList.TAB_FOLLOWERS);

    companion object {
        fun fromRoute(value: String?): FollowTab =
            entries.firstOrNull { it.routeValue == value } ?: Following
    }
}

/// 单个 tab 的列表分页状态
data class FollowListPageState(
    val items: List<FollowListItem> = emptyList(),
    val isInitialLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasMore: Boolean = true,
    val error: String? = null,
    val loaded: Boolean = false,
)

data class FollowListUiState(
    val selectedTab: FollowTab = FollowTab.Following,
    val following: FollowListPageState = FollowListPageState(),
    val followers: FollowListPageState = FollowListPageState(),
)

/**
 * 关注、粉丝列表页 ViewModel
 */
@HiltViewModel
class FollowListViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val followRepository: FollowRepository,
) : ViewModel() {

    private val userId: Long = savedStateHandle.get<Long>("userId") ?: 0L

    private val _uiState = MutableStateFlow(
        FollowListUiState(
            selectedTab = FollowTab.fromRoute(savedStateHandle.get<String>("tab")),
        )
    )
    val uiState: StateFlow<FollowListUiState> = _uiState.asStateFlow()

    /// 共享的关注关系
    val relations: StateFlow<Map<Long, FollowRelation>> = followRepository.relations

    private val cursors = mutableMapOf<FollowTab, String?>()

    init {
        loadInitial(_uiState.value.selectedTab)
    }

    /// 切换 tab
    fun selectTab(tab: FollowTab) {
        if (_uiState.value.selectedTab == tab) return
        _uiState.update { it.copy(selectedTab = tab) }
        loadInitial(tab)
    }

    /// 初始加载
    fun loadInitial(tab: FollowTab) {
        val page = pageOf(tab)
        if (page.loaded || page.isInitialLoading) return
        updatePage(tab) { it.copy(isInitialLoading = true, error = null) }
        viewModelScope.launch {
            fetch(tab, null).fold(
                onSuccess = { response ->
                    cursors[tab] = response.nextCursor
                    updatePage(tab) {
                        it.copy(
                            items = response.items,
                            isInitialLoading = false,
                            isLoadingMore = false,
                            hasMore = response.hasMore,
                            loaded = true,
                            error = null,
                        )
                    }
                    seedRelations(tab, response.items)
                },
                onFailure = { e ->
                    updatePage(tab) {
                        it.copy(isInitialLoading = false, isLoadingMore = false, error = e.message)
                    }
                },
            )
        }
    }

    /// 加载更多
    fun loadMore(tab: FollowTab) {
        val page = pageOf(tab)
        if (page.isLoadingMore || page.isInitialLoading || !page.hasMore) return
        updatePage(tab) { it.copy(isLoadingMore = true) }
        viewModelScope.launch {
            fetch(tab, cursors[tab]).fold(
                onSuccess = { response ->
                    cursors[tab] = response.nextCursor
                    updatePage(tab) {
                        it.copy(
                            items = it.items + response.items,
                            isLoadingMore = false,
                            hasMore = response.hasMore,
                        )
                    }
                    seedRelations(tab, response.items)
                },
                onFailure = {
                    updatePage(tab) { it.copy(isLoadingMore = false) }
                },
            )
        }
    }

    /// 关注
    fun follow(item: FollowListItem) {
        viewModelScope.launch { followRepository.follow(item.id) }
    }

    /// 取关
    fun unfollow(item: FollowListItem) {
        viewModelScope.launch { followRepository.unfollow(item.id) }
    }

    private fun pageOf(tab: FollowTab): FollowListPageState =
        if (tab == FollowTab.Following) _uiState.value.following else _uiState.value.followers

    private fun updatePage(tab: FollowTab, transform: (FollowListPageState) -> FollowListPageState) {
        _uiState.update { state ->
            if (tab == FollowTab.Following) state.copy(following = transform(state.following))
            else state.copy(followers = transform(state.followers))
        }
    }

    /// 拉取对应列表内容
    private suspend fun fetch(tab: FollowTab, cursor: String?): Result<FollowListResponse> {
        val cursorLong = cursor?.toLongOrNull()
        return if (tab == FollowTab.Following) {
            followRepository.getFollowing(userId, cursorLong)
        } else {
            followRepository.getFollowers(userId, cursorLong)
        }
    }

    /// 写入共享关注关系
    private fun seedRelations(tab: FollowTab, items: List<FollowListItem>) {
        items.forEach { item ->
            val relation = if (tab == FollowTab.Following) {
                FollowRelation(
                    following = item.isFollowing ?: true,
                    followedBack = item.isFollowedBack ?: false,
                )
            } else {
                FollowRelation(
                    following = item.isFollowing ?: false,
                    followedBack = item.isFollowedBack ?: true,
                )
            }
            followRepository.updateRelation(item.id, relation)
        }
    }
}
