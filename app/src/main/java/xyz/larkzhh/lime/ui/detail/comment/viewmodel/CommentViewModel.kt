package xyz.larkzhh.lime.ui.detail.comment.viewmodel

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
import xyz.larkzhh.lime.domain.repository.UserRepository
import android.net.Uri
import java.io.File
import javax.inject.Inject

/// 排序方式： 热度、时间
enum class CommentSort { HOT, TIME }

/// 已录制的语音
data class VoiceRecord(
    val file: File,
    val durationSeconds: Int,
)

data class CommentUiState(
    val comments: List<CommentData> = emptyList(),
    val commentCountDelta: Int = 0,// 总评论数增量
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasMore: Boolean = false,
    val nextCursor: String? = null,
    val sort: CommentSort = CommentSort.HOT,
    val isSubmitting: Boolean = false,
    val expandedReplies: Map<Long, ExpandedRepliesState> = emptyMap(), // 展开的回复
    val replyTarget: ReplyTarget? = null,// 当前评论框目标，null 评论笔记，非 null 回复某条评论
    val showInputSheet: Boolean = false,
    val pendingImages: List<Uri> = emptyList(),// 待发送的评论图片
    val pendingVoice: VoiceRecord? = null,// 待发送的语音
    val showVoiceSheet: Boolean = false,
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

/**
 * 笔记详情页中评论模块的 ViewModel。
 * 1. 管理评论列表、回复列表的 UI 状态。
 * 2. 处理用户交互逻辑。
 * 3. 图片/语音上传与文本内容的提交。
 */
@HiltViewModel
class CommentViewModel @Inject constructor(
    private val commentRepository: CommentRepository,
    private val userRepository: UserRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CommentUiState())
    val uiState = _uiState.asStateFlow()

    private var noteId: Long = 0L

    /// 当前登录用户id
    val currentUserId: Long? get() = userRepository.userFlow.value?.id

    /// 当前登录用户头像
    val currentUserAvatar: String? get() = userRepository.userFlow.value?.avatar

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

    /// 提交评论，图片和语音互斥
    fun submitComment(content: String) {
        val images = _uiState.value.pendingImages
        val voice = _uiState.value.pendingVoice
        if (content.isBlank() && images.isEmpty() && voice == null) return
        val target = _uiState.value.replyTarget
        _uiState.update { it.copy(isSubmitting = true) }
        viewModelScope.launch {
            // 上传图片
            val uploadedUrls = mutableListOf<String>()
            for (uri in images) {
                val result = commentRepository.uploadCommentImage(uri)
                result.onSuccess { uploadedUrls.add(it) }
                result.onFailure {
                    _uiState.update { it.copy(isSubmitting = false) }
                    return@launch
                }
            }

            // 上传语音
            var voiceUrl: String? = null
            if (voice != null) {
                commentRepository.uploadCommentVoice(voice.file)
                    .onSuccess { voiceUrl = it }
                    .onFailure {
                        _uiState.update { it.copy(isSubmitting = false) }
                        return@launch
                    }
            }

            val imageUrls = uploadedUrls.ifEmpty { null }
            val textContent = content.ifBlank { null }

            if (target == null) {
                commentRepository.sentComment(noteId, textContent, imageUrls, voiceUrl, voice?.durationSeconds)
                    .onSuccess { newComment ->
                        val comment = when {
                            !imageUrls.isNullOrEmpty() -> newComment.copy(images = imageUrls)
                            voiceUrl != null -> newComment.copy(voiceUrl = voiceUrl, voiceDuration = voice?.durationSeconds)
                            else -> newComment
                        }
                        _uiState.update { it.copy(
                            comments = listOf(comment) + it.comments,
                            commentCountDelta = it.commentCountDelta + 1,
                            isSubmitting = false,
                            showInputSheet = false,
                            replyTarget = null,
                            pendingImages = emptyList(),
                            pendingVoice = null,
                        ) }
                        voice?.file?.delete()
                    }
                    .onFailure { _uiState.update { it.copy(isSubmitting = false) } }
            } else {
                commentRepository.sentReply(noteId, target.commentId, textContent, imageUrls, target.replyToUserId, voiceUrl, voice?.durationSeconds)
                    .onSuccess { newReply ->
                        val reply = when {
                            !imageUrls.isNullOrEmpty() -> newReply.copy(images = imageUrls)
                            voiceUrl != null -> newReply.copy(voiceUrl = voiceUrl, voiceDuration = voice?.durationSeconds)
                            else -> newReply
                        }
                        _uiState.update { s ->
                            val commentId = target.commentId
                            val existing = s.expandedReplies[commentId]
                            val updatedExpandedReplies = if (existing != null) {
                                s.expandedReplies + (commentId to existing.copy(replies = existing.replies + reply))
                            } else {
                                s.expandedReplies// 没展开，不做处理
                            }
                            // 乐观追加到评论的顶部预览
                            val updatedComments = s.comments.map { c ->
                                if (c.id == commentId) c.copy(
                                    topReplies = (c.topReplies ?: emptyList()) + reply,
                                    replyCount = c.replyCount + 1,
                                ) else c
                            }
                            s.copy(
                                isSubmitting = false,
                                showInputSheet = false,
                                replyTarget = null,
                                pendingImages = emptyList(),
                                pendingVoice = null,
                                expandedReplies = updatedExpandedReplies,
                                comments = updatedComments,
                                commentCountDelta = s.commentCountDelta + 1,
                            )
                        }
                        voice?.file?.delete()
                        // 不刷新列表，重进详情页才同步服务端的排序
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

    /// 关闭输入面板，清空语音临时文件
    fun closeInputSheet() {
        _uiState.value.pendingVoice?.file?.delete()
        _uiState.update { it.copy(showInputSheet = false, replyTarget = null, pendingImages = emptyList(), pendingVoice = null) }
    }

    /// 添加待发送的评论图片
    fun addCommentImages(uris: List<Uri>) {
        _uiState.update { s ->
            val merged = (s.pendingImages + uris).take(9)
            s.copy(pendingImages = merged)
        }
    }

    /// 移除一张待发送的图片
    fun removeCommentImage(uri: Uri) {
        _uiState.update { it.copy(pendingImages = it.pendingImages - uri) }
    }

    /// 打开录音面板
    fun openVoiceSheet() {
        _uiState.update { it.copy(showVoiceSheet = true, showInputSheet = false) }
    }

    /// 关闭录音面板
    fun closeVoiceSheet() {
        _uiState.update { it.copy(showVoiceSheet = false, showInputSheet = true) }
    }

    /// 完成录音，保存语音记录，恢复评论框
    fun setPendingVoice(voice: VoiceRecord) {
        _uiState.update { it.copy(pendingVoice = voice, showVoiceSheet = false, showInputSheet = true) }
    }

    /// 移除待发送的语音
    fun removePendingVoice() {
        _uiState.value.pendingVoice?.file?.delete()
        _uiState.update { it.copy(pendingVoice = null) }
    }

    /// 删除评论，乐观移除
    fun deleteComment(commentId: Long) {
        val backup = _uiState.value
        _uiState.update { s -> s.copy(
            comments = s.comments.filter { c -> c.id != commentId },
            commentCountDelta = s.commentCountDelta - 1,
        ) }
        viewModelScope.launch {
            commentRepository.deleteComment(commentId).onFailure {
                _uiState.update { backup }
            }
        }
    }

    /// 删除回复
    fun deleteReply(commentId: Long, replyId: Long) {
        val backup = _uiState.value
        _uiState.update { s ->
            s.copy(
                comments = s.comments.map { c ->
                    // 定位父评论
                    if (c.id == commentId) c.copy(
                        topReplies = c.topReplies?.filter { r -> r.id != replyId },
                        replyCount = (c.replyCount - 1).coerceAtLeast(0),
                    ) else c
                },
                // 更新展开的回复列表
                expandedReplies = s.expandedReplies.mapValues { (id, state) ->
                    if (id == commentId) state.copy(replies = state.replies.filter { r -> r.id != replyId })
                    else state
                },
                commentCountDelta = s.commentCountDelta - 1,
            )
        }
        viewModelScope.launch {
            commentRepository.deleteComment(replyId).onFailure {
                _uiState.update { backup }
            }
        }
    }
}
