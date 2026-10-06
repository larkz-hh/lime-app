package xyz.larkzhh.lime.ui.video.feed

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tencent.mmkv.MMKV
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import xyz.larkzhh.lime.data.network.model.FeedAuthor
import xyz.larkzhh.lime.data.network.model.FeedItem
import xyz.larkzhh.lime.data.network.model.NoteDetailData
import xyz.larkzhh.lime.data.network.model.VideoInfo
import xyz.larkzhh.lime.data.network.model.VideoOrientation
import xyz.larkzhh.lime.data.network.model.orientationEnum
import xyz.larkzhh.lime.domain.NoteEvent
import xyz.larkzhh.lime.domain.NoteEventBus
import xyz.larkzhh.lime.domain.model.FollowRelation
import xyz.larkzhh.lime.domain.repository.FollowRepository
import xyz.larkzhh.lime.domain.repository.NoteRepository
import xyz.larkzhh.lime.ui.video.VideoFeedSessionStore
import xyz.larkzhh.lime.navigation.route.Screen
import xyz.larkzhh.lime.util.system.NetworkMonitor
import javax.inject.Inject

/// 视频页模型
data class VideoItem(
    val id: Long,
    val title: String?,
    val body: String?,
    val author: FeedAuthor,
    val video: VideoInfo,
    val liked: Boolean,
    val likeCount: Int,
    val favorited: Boolean,
    val favCount: Int,
    val commentCount: Int,
    val hydrated: Boolean = false,// 是否已补水
) {
    val isLandscape: Boolean get() = video.orientationEnum() == VideoOrientation.LANDSCAPE
}

data class VideoFeedUiState(
    val items: List<VideoItem> = emptyList(),
    val currentIndex: Int = 0,
    val isLoading: Boolean = true,
    val isLoadingMore: Boolean = false,
    val isRefreshing: Boolean = false,
    val hasMore: Boolean = false,
    val fullscreen: Boolean = false,
    val pausedNoteIds: Set<Long> = emptySet(),
    val error: String? = null,
    // 横屏全屏会话
    val landscapeItems: List<VideoItem> = emptyList(),
    val landscapeIndex: Int = 0,
    val isLandscapeLoading: Boolean = false,
    val landscapeHasMore: Boolean = false,
    val pendingScrollTarget: Int? = null,
    // 播放偏好
    val playbackSpeed: Float = 1f,// 播放倍速
    val autoPlayNext: Boolean = false,// 自动连播
    val backgroundAudio: Boolean = false,// 后台继续播放
    val clearScreen: Boolean = false,// 清屏播放
    val danmakuOpacity: Float = 1f,// 弹幕不透明度
)

/// 信息流来源
sealed interface FeedSource {
    data class Recommendation(val seedNoteId: Long) : FeedSource// 推荐
    data object PersonalList : FeedSource// 个人
}

private fun FeedItem.toVideoItemOrNull(): VideoItem? {
    val v = video ?: return null
    return VideoItem(
        id = id,
        title = title,
        body = null,
        author = author,
        video = v,
        liked = liked,
        likeCount = likeCount,
        favorited = false,
        favCount = 0,
        commentCount = 0,
        hydrated = false,
    )
}

@HiltViewModel
class VideoFeedViewModel @Inject constructor(
    private val noteRepository: NoteRepository,
    private val eventBus: NoteEventBus,
    private val followRepository: FollowRepository,
    networkMonitor: NetworkMonitor,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val mmkv by lazy { MMKV.defaultMMKV() }

    private val reportedViewIds = mutableSetOf<Long>() // 本会话内已上报浏览的视频 id

    val isUnmetered: StateFlow<Boolean> = networkMonitor.isUnmetered

    /// 共享关注关系
    val relations = followRepository.relations

    // 后台继续播放偏好
    private val _uiState = MutableStateFlow(
        VideoFeedUiState(backgroundAudio = mmkv.decodeBool(KEY_BACKGROUND_AUDIO, false)),
    )
    val uiState: StateFlow<VideoFeedUiState> = _uiState.asStateFlow()

    private val noteId: Long = savedStateHandle.get<Long>("noteId") ?: 0L
    private val sourceArg: String =
        savedStateHandle.get<String>("source") ?: Screen.VideoFeed.SOURCE_RECOMMENDATION
    // tab 栏进入
    val isTabEntry: Boolean get() = noteId <= 0L || sourceArg == Screen.VideoFeed.SOURCE_TAB

    private var source: FeedSource = FeedSource.Recommendation(noteId)
    private var cursor: Long? = null

    init {
        loadInitial()
        observeNoteEvents()
    }

    private fun loadInitial() {
        // 个人列表，优先读取会话缓存
        if (sourceArg == Screen.VideoFeed.SOURCE_PERSONAL) {
            val payload = VideoFeedSessionStore.take(noteId)
            if (payload != null) {
                source = FeedSource.PersonalList
                val videos = payload.items.mapNotNull { it.toVideoItemOrNull() }// 过滤无效数据
                val startId = payload.items.getOrNull(payload.startIndex)?.id ?: noteId
                val start = videos.indexOfFirst { it.id == startId }.coerceAtLeast(0)
                _uiState.update {
                    it.copy(
                        items = videos,
                        currentIndex = start,
                        isLoading = false,
                        hasMore = false,
                    )
                }
                hydrateAround(start)
                reportCurrentViewOnce()
                return
            }
        }
        source = FeedSource.Recommendation(noteId)// 取不到降级为推荐流
        loadRecommendationFirst()
    }

    private fun loadRecommendationFirst() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            cursor = null
            val seed = noteId.takeIf { it > 0 }
            noteRepository.getVideoFeed(cursor = null, seedNoteId = seed, orientation = null).fold(
                onSuccess = { response ->
                    cursor = response.nextCursor
                    val videos = response.items.mapNotNull { it.toVideoItemOrNull() }
                    val start = if (seed != null) videos.indexOfFirst { it.id == noteId }.coerceAtLeast(0) else 0
                    _uiState.update {
                        it.copy(
                            items = videos,
                            currentIndex = start,
                            isLoading = false,
                            hasMore = response.hasMore,
                        )
                    }
                    hydrateAround(start)
                    reportCurrentViewOnce()
                },
                onFailure = { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                },
            )
        }
    }

    fun loadMore() {
        val state = _uiState.value
        if (source !is FeedSource.Recommendation) return
        if (state.isLoadingMore || !state.hasMore || state.isLoading) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true) }
            noteRepository.getVideoFeed(cursor = cursor, seedNoteId = null, orientation = null).fold(
                onSuccess = { response ->
                    cursor = response.nextCursor
                    val existing = _uiState.value.items.map { it.id }.toSet()
                    val more = response.items.mapNotNull { it.toVideoItemOrNull() }
                        .filter { it.id !in existing }
                    _uiState.update {
                        it.copy(
                            isLoadingMore = false,
                            items = it.items + more,
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

    /// 下拉刷新
    fun refresh() {
        val state = _uiState.value
        if (state.isLoading || state.isRefreshing) return
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true, error = null) }
            cursor = null
            val seed = noteId.takeIf { it > 0 }
            noteRepository.getVideoFeed(cursor = null, seedNoteId = seed, orientation = null).fold(
                onSuccess = { response ->
                    cursor = response.nextCursor
                    val videos = response.items.mapNotNull { it.toVideoItemOrNull() }
                    val start = if (seed != null) videos.indexOfFirst { it.id == noteId }.coerceAtLeast(0) else 0
                    _uiState.update {
                        it.copy(
                            items = videos,
                            currentIndex = start,
                            pendingScrollTarget = start,
                            isRefreshing = false,
                            hasMore = response.hasMore,
                        )
                    }
                    hydrateAround(start)
                    reportCurrentViewOnce()
                },
                onFailure = { e ->
                    _uiState.update { it.copy(isRefreshing = false, error = e.message) }
                },
            )
        }
    }

    /// 写入共享关注关系
    fun seedFollowRelation(author: FeedAuthor) {
        followRepository.updateRelation(
            author.id,
            FollowRelation(author.isFollowing ?: false, author.isFollowedBack ?: false),
        )
    }

    /// 关注作者
    fun followAuthor() {
        val authorId = _uiState.value.items.getOrNull(_uiState.value.currentIndex)?.author?.id ?: return
        viewModelScope.launch { followRepository.follow(authorId) }
    }

    /// 取消关注作者
    fun unfollowAuthor() {
        val authorId = _uiState.value.items.getOrNull(_uiState.value.currentIndex)?.author?.id ?: return
        viewModelScope.launch { followRepository.unfollow(authorId) }
    }

    /// 切页
    fun onPageSettled(index: Int) {
        _uiState.update { it.copy(currentIndex = index) }
        val state = _uiState.value
        if (index >= state.items.size - 2) loadMore()
        hydrateAround(index)
        reportCurrentViewOnce()// 进入该视频上报一次浏览
    }

    /// 对当前页未补水的前后项拉取笔记详情
    private fun hydrateAround(index: Int) {
        val items = _uiState.value.items
        for (i in (index - 1)..(index + 1)) {
            val item = items.getOrNull(i) ?: continue
            if (item.hydrated) continue
            hydrate(item.id)
        }
    }

    private fun hydrate(id: Long) {
        viewModelScope.launch {
            noteRepository.getNoteDetail(id, noView = true).onSuccess { detail ->
                _uiState.update { state ->
                    state.copy(items = state.items.map { it.mergeDetail(id, detail) })
                }
            }
        }
    }

    /// 上报一次浏览
    private fun reportViewOnce(id: Long) {
        if (id <= 0L || !reportedViewIds.add(id)) return
        viewModelScope.launch {
            noteRepository.getNoteDetail(id).onFailure {}
        }
    }

    /// 竖屏流当前播放页
    private fun reportCurrentViewOnce() {
        val state = _uiState.value
        state.items.getOrNull(state.currentIndex)?.id?.let(::reportViewOnce)
    }

    /// 横屏流当前播放页
    private fun reportLandscapeCurrentViewOnce() {
        val state = _uiState.value
        state.landscapeItems.getOrNull(state.landscapeIndex)?.id?.let(::reportViewOnce)
    }

    private fun VideoItem.mergeDetail(id: Long, detail: NoteDetailData): VideoItem {
        if (this.id != id) return this
        return copy(
            title = detail.title ?: title,
            body = detail.content,
            liked = detail.liked,
            likeCount = detail.likeCount,
            favorited = detail.favorited,
            favCount = detail.favCount,
            commentCount = detail.commentCount,
            hydrated = true,// 补水完成
        )
    }

    fun toggleLike() {
        val state = _uiState.value
        val item = state.items.getOrNull(state.currentIndex) ?: return
        toggleLikeById(item.id)
    }

    fun toggleLikeById(id: Long) {
        _uiState.update { s -> s.copy(items = s.items.turnLike(id), landscapeItems = s.landscapeItems.turnLike(id)) }
        viewModelScope.launch {
            val nowLiked = _uiState.value.likeStateOf(id) ?: return@launch
            val result = if (!nowLiked) noteRepository.unlikeNote(id) else noteRepository.likeNote(id)
            result.fold(
                onSuccess = {
                    val liked = _uiState.value.likeStateOf(id) ?: return@fold
                    val count = _uiState.value.likeCountOf(id) ?: return@fold
                    eventBus.emit(NoteEvent.LikeChanged(id, liked, count))
                },
                onFailure = {
                    // 回滚
                    _uiState.update { s -> s.copy(items = s.items.turnLike(id), landscapeItems = s.landscapeItems.turnLike(id)) }
                },
            )
        }
    }

    fun toggleFavorite() {
        val state = _uiState.value
        val item = state.items.getOrNull(state.currentIndex) ?: return
        toggleFavoriteById(item.id)
    }
    
    fun toggleFavoriteById(id: Long) {
        _uiState.update { s -> s.copy(items = s.items.turnFav(id), landscapeItems = s.landscapeItems.turnFav(id)) }
        viewModelScope.launch {
            val nowFav = _uiState.value.favStateOf(id) ?: return@launch
            val result = if (!nowFav) noteRepository.unfavoriteNote(id) else noteRepository.favoriteNote(id)
            result.fold(
                onSuccess = {
                    val fav = _uiState.value.favStateOf(id) ?: return@fold
                    val count = _uiState.value.favCountOf(id) ?: return@fold
                    eventBus.emit(NoteEvent.FavoriteChanged(id, fav, count))
                },
                onFailure = {
                    _uiState.update { s -> s.copy(items = s.items.turnFav(id), landscapeItems = s.landscapeItems.turnFav(id)) }
                },
            )
        }
    }

    /// 同步
    private fun observeNoteEvents() {
        viewModelScope.launch {
            eventBus.events.collect { event ->
                when (event) {
                    is NoteEvent.LikeChanged -> _uiState.update { s ->
                        s.copy(
                            items = s.items.map {
                                if (it.id == event.noteId) it.copy(liked = event.liked, likeCount = event.likeCount) else it
                            },
                            landscapeItems = s.landscapeItems.map {
                                if (it.id == event.noteId) it.copy(liked = event.liked, likeCount = event.likeCount) else it
                            },
                        )
                    }
                    is NoteEvent.FavoriteChanged -> _uiState.update { s ->
                        s.copy(
                            items = s.items.map {
                                if (it.id == event.noteId) it.copy(favorited = event.favorited, favCount = event.favCount) else it
                            },
                            landscapeItems = s.landscapeItems.map {
                                if (it.id == event.noteId) it.copy(favorited = event.favorited, favCount = event.favCount) else it
                            },
                        )
                    }
                    is NoteEvent.CommentCountChanged -> _uiState.update { s ->
                        s.copy(
                            items = s.items.map {
                                if (it.id == event.noteId) it.copy(commentCount = it.commentCount + event.delta) else it
                            },
                            landscapeItems = s.landscapeItems.map {
                                if (it.id == event.noteId) it.copy(commentCount = it.commentCount + event.delta) else it
                            },
                        )
                    }
                }
            }
        }
    }

    // 横屏全屏
    private var landscapeCursor: Long? = null
    private var landscapeEntryIndex: Int = 0// 进入横屏时主队列下标
    private var landscapeEntrySessionIndex: Int = 0// 进入横屏时会话内下标

    /// 进入横屏
    fun enterFullscreen() {
        val state = _uiState.value
        val current = state.items.getOrNull(state.currentIndex) ?: return
        landscapeEntryIndex = state.currentIndex
        when (source) {
            is FeedSource.PersonalList -> {
                val ls = state.items.filter { it.isLandscape }
                val idx = ls.indexOfFirst { it.id == current.id }.coerceAtLeast(0)
                landscapeEntrySessionIndex = idx
                _uiState.update {
                    it.copy(
                        fullscreen = true,
                        landscapeItems = ls,
                        landscapeIndex = idx,
                        landscapeHasMore = false,
                    )
                }
            }
            is FeedSource.Recommendation -> {
                landscapeEntrySessionIndex = 0
                _uiState.update {
                    it.copy(
                        fullscreen = true,
                        landscapeItems = listOf(current),
                        landscapeIndex = 0,
                        isLandscapeLoading = true,
                        landscapeHasMore = false,
                    )
                }
                viewModelScope.launch {
                    landscapeCursor = null
                    noteRepository.getVideoFeed(cursor = null, seedNoteId = current.id, orientation = "landscape").fold(
                        onSuccess = { response ->
                            landscapeCursor = response.nextCursor
                            val videos = response.items.mapNotNull { it.toVideoItemOrNull() }.filter { it.isLandscape }// 过滤
                            // 保证当前视频在首位
                            val list = if (videos.any { it.id == current.id }) videos else listOf(current) + videos
                            val idx = list.indexOfFirst { it.id == current.id }.coerceAtLeast(0)
                            landscapeEntrySessionIndex = idx
                            _uiState.update {
                                it.copy(
                                    landscapeItems = list,
                                    landscapeIndex = idx,
                                    isLandscapeLoading = false,
                                    landscapeHasMore = response.hasMore,
                                )
                            }
                        },
                        onFailure = {
                            _uiState.update { it.copy(isLandscapeLoading = false) }
                        },
                    )
                }
            }
        }
    }

    /// 横屏切页
    fun onLandscapePageSettled(index: Int) {
        _uiState.update { it.copy(landscapeIndex = index) }
        val state = _uiState.value
        if (source is FeedSource.Recommendation && index >= state.landscapeItems.size - 2) {
            loadMoreLandscape()
        }
        reportLandscapeCurrentViewOnce()
    }

    private fun loadMoreLandscape() {
        val state = _uiState.value
        if (source !is FeedSource.Recommendation) return
        if (state.isLandscapeLoading || !state.landscapeHasMore) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLandscapeLoading = true) }
            noteRepository.getVideoFeed(cursor = landscapeCursor, seedNoteId = null, orientation = "landscape").fold(
                onSuccess = { response ->
                    landscapeCursor = response.nextCursor
                    val existing = _uiState.value.landscapeItems.map { it.id }.toSet()
                    val more = response.items.mapNotNull { it.toVideoItemOrNull() }
                        .filter { it.isLandscape && it.id !in existing }// 去重
                    _uiState.update {
                        it.copy(
                            isLandscapeLoading = false,
                            landscapeItems = it.landscapeItems + more,
                            landscapeHasMore = response.hasMore,
                        )
                    }
                },
                onFailure = {
                    _uiState.update { it.copy(isLandscapeLoading = false) }
                },
            )
        }
    }

    /// 退出横屏，推荐流回主队列并停在当前，个人队列只移动游标
    fun exitFullscreen() {
        val state = _uiState.value
        val landscapeCurrent = state.landscapeItems.getOrNull(state.landscapeIndex)
        when (source) {
            is FeedSource.PersonalList -> {
                val mainIdx = landscapeCurrent?.let { lc -> state.items.indexOfFirst { it.id == lc.id } } ?: -1
                val target = if (mainIdx >= 0) mainIdx else state.currentIndex// 移动游标
                _uiState.update {
                    it.copy(
                        fullscreen = false,
                        currentIndex = target,
                        pendingScrollTarget = target,
                        landscapeItems = emptyList(),
                    )
                }
            }
            is FeedSource.Recommendation -> {
                // 浏览的横视频写入主队列
                val session = state.landscapeItems
                val entrySessionIdx = landscapeEntrySessionIndex.coerceIn(0, (session.size - 1).coerceAtLeast(0))
                val browsedTail = if (state.landscapeIndex > entrySessionIdx) {
                    session.subList(entrySessionIdx + 1, (state.landscapeIndex + 1).coerceAtMost(session.size))
                } else {
                    emptyList()
                }
                val browsedIds = browsedTail.map { it.id }.toSet()
                val existing = state.items.map { it.id }.toSet()// 主队列已存在
                val newRest = session.filter { it.id !in browsedIds && it.id !in existing }// 新加视频
                val moved = browsedTail + newRest
                val movedIds = moved.map { it.id }.toSet()
                val base = state.items.filter { it.id !in movedIds }// 入口视频保留原位
                val insertAt = (landscapeEntryIndex + 1).coerceIn(0, base.size)
                val newItems = base.toMutableList().apply { addAll(insertAt, moved) }// 新队列
                val targetId = landscapeCurrent?.id
                val newIndex = targetId?.let { id -> newItems.indexOfFirst { it.id == id } }
                    ?.takeIf { it >= 0 } ?: state.currentIndex// 重新定位
                _uiState.update {
                    it.copy(
                        fullscreen = false,
                        items = newItems,
                        currentIndex = newIndex,
                        pendingScrollTarget = newIndex,
                        landscapeItems = emptyList(),
                    )
                }
            }
        }
    }

    /// 消费跳转目标
    fun consumePendingScroll() = _uiState.update { it.copy(pendingScrollTarget = null) }

    /// 暂停
    fun togglePaused(noteId: Long) = _uiState.update { state ->
        state.setPaused(noteId, noteId !in state.pausedNoteIds)
    }

    /// 直接设置暂停状态
    fun setPaused(noteId: Long, paused: Boolean) = _uiState.update { state ->
        state.setPaused(noteId, paused)
    }

    /// 设置播放倍速
    fun setPlaybackSpeed(speed: Float) = _uiState.update { it.copy(playbackSpeed = speed) }

    /// 切换自动连播
    fun toggleAutoPlayNext() = _uiState.update { it.copy(autoPlayNext = !it.autoPlayNext) }

    /// 切换后台继续播放音频
    fun toggleBackgroundAudio() = _uiState.update {
        val next = !it.backgroundAudio
        mmkv.encode(KEY_BACKGROUND_AUDIO, next)
        it.copy(backgroundAudio = next)
    }

    /// 切换清屏播放
    fun toggleClearScreen() = _uiState.update { it.copy(clearScreen = !it.clearScreen) }

    /// 设置弹幕不透明度
    fun setDanmakuOpacity(opacity: Float) =
        _uiState.update { it.copy(danmakuOpacity = opacity.coerceIn(0.2f, 1f)) }

    /// 删除当前视频
    fun deleteCurrent(onResult: (Boolean) -> Unit) {
        val idx = _uiState.value.currentIndex
        val item = _uiState.value.items.getOrNull(idx) ?: run {
            onResult(false)
            return
        }
        viewModelScope.launch {
            noteRepository.deleteNote(item.id)
                .onSuccess {
                    _uiState.update { state ->
                        val items = state.items.filterNot { it.id == item.id }
                        val landscapeItems = state.landscapeItems.filterNot { it.id == item.id }
                        val target = if (items.isEmpty()) 0 else minOf(idx, items.lastIndex)
                        state.copy(
                            items = items,
                            landscapeItems = landscapeItems,
                            currentIndex = target,
                            pendingScrollTarget = target,
                            hasMore = if (items.isEmpty()) false else state.hasMore,
                        )
                    }
                    onResult(true)
                    if (_uiState.value.items.isEmpty() && source is FeedSource.Recommendation) {
                        loadRecommendationFirst()// 队列空后重新拉最新
                    }
                }
                .onFailure { onResult(false) }
        }
    }

    companion object {
        private const val KEY_BACKGROUND_AUDIO = "video.background_audio"
    }
}

/// 点赞或取消
private fun List<VideoItem>.turnLike(id: Long): List<VideoItem> = map {
    if (it.id == id) it.copy(liked = !it.liked, likeCount = if (it.liked) it.likeCount - 1 else it.likeCount + 1) else it
}

/// 收藏或取消
private fun List<VideoItem>.turnFav(id: Long): List<VideoItem> = map {
    if (it.id == id) it.copy(favorited = !it.favorited, favCount = if (it.favorited) it.favCount - 1 else it.favCount + 1) else it
}

/// 从队列取某 id 的项
private fun VideoFeedUiState.find(id: Long): VideoItem? =
    items.firstOrNull { it.id == id } ?: landscapeItems.firstOrNull { it.id == id }

private fun VideoFeedUiState.setPaused(noteId: Long, paused: Boolean): VideoFeedUiState =
    copy(pausedNoteIds = if (paused) pausedNoteIds + noteId else pausedNoteIds - noteId)
private fun VideoFeedUiState.likeStateOf(id: Long): Boolean? = find(id)?.liked
private fun VideoFeedUiState.likeCountOf(id: Long): Int? = find(id)?.likeCount
private fun VideoFeedUiState.favStateOf(id: Long): Boolean? = find(id)?.favorited
private fun VideoFeedUiState.favCountOf(id: Long): Int? = find(id)?.favCount