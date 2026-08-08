package xyz.larkzhh.lime.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import xyz.larkzhh.lime.data.network.model.CommentData
import xyz.larkzhh.lime.data.network.model.ReplyData
import xyz.larkzhh.lime.domain.repository.CommentRepository
import javax.inject.Inject

/// 排序方式： 热度、时间
enum class CommentSort { HOT, TIME }

data class CommentUiState(
    val comments: List<CommentData> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasMore: Boolean = false,
    val nextCursor: String? = null,
    val sort: CommentSort = CommentSort.HOT,
    val isSubmitting: Boolean = false,
    val expandedReplies: Map<Long, ExpandedRepliesState> = emptyMap(), // 展开的回复
    val replyTarget: ReplyTarget? = null,// 当前评论框目标，null 评论笔记，非 null 回复某条评论
    val showInputSheet: Boolean = false,
)

/// 单条评论回复展开
data class ExpandedRepliesState(
    val replies: List<ReplyData> = emptyList(),
    val hasMore: Boolean = false,
    val nextCursor: Long? = null,
    val isLoading: Boolean = false,
)

/// 回复目标
data class ReplyTarget(
    val commentId: Long,
    val replyToUserId: Long?,
    val replyToNickname: String,
)

@HiltViewModel
class CommentViewModel @Inject constructor(
    private val commentRepository: CommentRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CommentUiState())
    val uiState = _uiState.asStateFlow()

    private var noteId: Long = 0L

    fun init(noteId: Long) {
        if (this.noteId == noteId) return
        this.noteId = noteId
        loadComments(refresh = true)
    }

    /// 切换评论排序方式
    fun setSort(sort: CommentSort) {
        if (_uiState.value.sort == sort) return
        _uiState.update { it.copy(sort = sort, comments = emptyList(), nextCursor = null) }
        loadComments(refresh = true)
    }

    /// 加载评论
    fun loadComments(refresh: Boolean = false) {
        val state = _uiState.value
        if (!refresh && (!state.hasMore || state.isLoadingMore)) return
        viewModelScope.launch {
            if (refresh) {
                _uiState.update { it.copy(isLoading = true) }
            } else {
                _uiState.update { it.copy(isLoadingMore = true) }
            }
            val cursor = if (refresh) null else state.nextCursor
            val sort = if (_uiState.value.sort == CommentSort.HOT) "hot" else "time"
            commentRepository.getComments(noteId, sort, cursor, size = 10)
                .onSuccess { result ->
                    _uiState.update { s ->
                        val newList = if (refresh) result.items else s.comments + result.items
                        s.copy(
                            comments = newList,
                            hasMore = result.hasMore,
                            nextCursor = result.nextCursor,
                            isLoading = false,
                            isLoadingMore = false,
                        )
                    }
                }
                .onFailure {
                    _uiState.update { it.copy(isLoading = false, isLoadingMore = false) }
                }
        }
    }

    /// 提交评论
    fun submitComment(content: String) {
        if (content.isBlank()) return
        val target = _uiState.value.replyTarget
        _uiState.update { it.copy(isSubmitting = true) }
        viewModelScope.launch {
            if (target == null) {
                commentRepository.sentComment(noteId, content)
                    .onSuccess { newComment ->
                        _uiState.update { it.copy(
                            comments = listOf(newComment) + it.comments,
                            isSubmitting = false,
                            showInputSheet = false,
                            replyTarget = null,// 清空当前的回复目标
                        ) }
                    }
                    .onFailure { _uiState.update { it.copy(isSubmitting = false) } }
            } else {
                commentRepository.sentReply(noteId, target.commentId, content, target.replyToUserId)
                    .onSuccess {
                        _uiState.update { s ->
                            s.copy(
                                isSubmitting = false,
                                showInputSheet = false,
                                replyTarget = null,// 清空当前的回复目标
                            )
                        }
                        loadComments(refresh = true)
                    }
                    .onFailure { _uiState.update { it.copy(isSubmitting = false) } }
            }
        }
    }

    /// 加载回复
    fun loadMoreReplies(commentId: Long) {
        val existing = _uiState.value.expandedReplies[commentId]
        if (existing?.isLoading == true) return
        if (existing != null && !existing.hasMore) return
        viewModelScope.launch {
            _uiState.update { s ->
                s.copy(expandedReplies = s.expandedReplies + (commentId to (existing ?: ExpandedRepliesState()).copy(isLoading = true)))
            }
            commentRepository.getReplies(commentId, existing?.nextCursor, size = 5)
                .onSuccess { result ->
                    _uiState.update { s ->
                        val prev = s.expandedReplies[commentId] ?: ExpandedRepliesState()
                        s.copy(expandedReplies = s.expandedReplies + (commentId to prev.copy(
                            replies = prev.replies + result.items,
                            hasMore = result.hasMore,
                            nextCursor = result.nextCursor,
                            isLoading = false,
                        )))
                    }
                }
                .onFailure {
                    _uiState.update { s ->
                        val prev = s.expandedReplies[commentId] ?: ExpandedRepliesState()
                        s.copy(expandedReplies = s.expandedReplies + (commentId to prev.copy(isLoading = false)))
                    }
                }
        }
    }

    /// 点赞评论
    fun toggleCommentLike(commentId: Long) {
        val comment = _uiState.value.comments.find { it.id == commentId } ?: return
        viewModelScope.launch {
            _uiState.update { s ->
                s.copy(comments = s.comments.map { c ->
                    if (c.id == commentId) c.copy(
                        liked = !c.liked,
                        likeCount = if (c.liked) c.likeCount - 1 else c.likeCount + 1,
                    ) else c
                })
            }
            val result = if (comment.liked) commentRepository.unlikeComment(commentId)
                         else commentRepository.likeComment(commentId)
            result.onFailure {
                _uiState.update { s ->
                    s.copy(comments = s.comments.map { c ->
                        if (c.id == commentId) comment else c
                    })
                }
            }
        }
    }

    /// 点赞回复
    fun toggleReplyLike(commentId: Long, replyId: Long) {
        val reply = _uiState.value.expandedReplies[commentId]?.replies?.find { it.id == replyId } ?: return
        viewModelScope.launch {
            _uiState.update { s ->
                val existing = s.expandedReplies[commentId] ?: return@update s
                s.copy(expandedReplies = s.expandedReplies + (commentId to existing.copy(
                    replies = existing.replies.map { r ->
                        if (r.id == replyId) r.copy(
                            liked = !r.liked,
                            likeCount = if (r.liked) r.likeCount - 1 else r.likeCount + 1,
                        ) else r
                    }
                )))
            }
            val result = if (reply.liked) commentRepository.unlikeComment(replyId)
                         else commentRepository.likeComment(replyId)
            result.onFailure {
                _uiState.update { s ->
                    val existing = s.expandedReplies[commentId] ?: return@update s
                    s.copy(expandedReplies = s.expandedReplies + (commentId to existing.copy(
                        replies = existing.replies.map { r -> if (r.id == replyId) reply else r }
                    )))
                }
            }
        }
    }

    /// 打开输入面板
    fun openInputSheet(replyTarget: ReplyTarget? = null) {
        _uiState.update { it.copy(showInputSheet = true, replyTarget = replyTarget) }
    }

    /// 关闭输入面板
    fun closeInputSheet() {
        _uiState.update { it.copy(showInputSheet = false, replyTarget = null) }
    }
}
