package xyz.larkzhh.lime.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import xyz.larkzhh.lime.R
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import xyz.larkzhh.lime.data.network.model.NoteDetailData
import xyz.larkzhh.lime.ui.components.CommentInputSheet
import xyz.larkzhh.lime.ui.components.SelectableText
import xyz.larkzhh.lime.ui.components.SelectionAction
import xyz.larkzhh.lime.ui.detail.components.AuthorBar
import xyz.larkzhh.lime.ui.detail.components.CommentCard
import xyz.larkzhh.lime.ui.detail.components.CommentHeader
import xyz.larkzhh.lime.ui.detail.components.ImagePreviewOverlay
import xyz.larkzhh.lime.ui.detail.components.NoteBottomBar
import xyz.larkzhh.lime.ui.detail.components.NoteImagePager
import xyz.larkzhh.lime.ui.theme.LimeDark
import xyz.larkzhh.lime.ui.theme.LimeGray
import xyz.larkzhh.lime.ui.theme.LimeLightGray
import xyz.larkzhh.lime.ui.theme.LimePrimary
import xyz.larkzhh.lime.util.copyToClipboard
import xyz.larkzhh.lime.util.formatRelativeTime
import xyz.larkzhh.lime.util.showToast

@Composable
fun DetailScreen(
    navController: NavHostController,
    noteId: String,
    viewModel: DetailViewModel = hiltViewModel(),
    commentViewModel: CommentViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val commentUiState by commentViewModel.uiState.collectAsState()

    LaunchedEffect(noteId) {
        val id = noteId.toLongOrNull() ?: return@LaunchedEffect
        viewModel.loadNote(id)
        commentViewModel.init(id)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(color = LimePrimary)
                    }
                }

                uiState.error != null -> {
                    Box(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(text = uiState.error ?: "加载失败", color = LimeGray, fontSize = 14.sp)
                    }
                }

                uiState.note != null -> {
                    AuthorBar(
                        note = uiState.note!!,
                        onBack = { navController.popBackStack() },
                    )
                    NoteContent(
                        note = uiState.note!!,
                        commentUiState = commentUiState,
                        modifier = Modifier.weight(1f),
                        onImageClick = viewModel::showImagePreview,
                        onSortChange = commentViewModel::setSort,
                        onLoadMoreComments = { commentViewModel.loadComments() },
                        onCommentLike = commentViewModel::toggleCommentLike,
                        onReply = { commentViewModel.openInputSheet(it) },
                        onLoadMoreReplies = commentViewModel::loadMoreReplies,
                        onReplyLike = commentViewModel::toggleReplyLike,
                    )
                    NoteBottomBar(
                        note = uiState.note!!,
                        onToggleLike = viewModel::toggleLike,
                        onToggleFavorite = viewModel::toggleFavorite,
                        onCommentClick = { commentViewModel.openInputSheet(null) },
                    )
                }
            }
        }

        // 图片全屏预览浮层
        val previewIndex = uiState.previewImageIndex
        val previewNote = uiState.note
        if (previewIndex != null && previewNote != null) {
            Dialog(
                onDismissRequest = viewModel::hideImagePreview,
                properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
            ) {
                ImagePreviewOverlay(
                    images = previewNote.images,
                    initialIndex = previewIndex,
                    onDismiss = viewModel::hideImagePreview,
                )
            }
        }

        // 评论输入框
        if (commentUiState.showInputSheet) {
            val hint = commentUiState.replyTarget?.let { "回复 @${it.replyToNickname}" } ?: "说点什么…"
            CommentInputSheet(
                hint = hint,
                isSubmitting = commentUiState.isSubmitting,
                onSubmit = commentViewModel::submitComment,
                onDismiss = commentViewModel::closeInputSheet,
            )
        }
    }
}

@Composable
private fun NoteContent(
    note: NoteDetailData,
    commentUiState: CommentUiState,
    onImageClick: (Int) -> Unit,
    onSortChange: (CommentSort) -> Unit,
    onLoadMoreComments: () -> Unit,
    onCommentLike: (Long) -> Unit,
    onReply: (ReplyTarget) -> Unit,
    onLoadMoreReplies: (Long) -> Unit,
    onReplyLike: (commentId: Long, replyId: Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val selectionActions = remember(context) {
        listOf(
            SelectionAction("复制", Icons.Outlined.ContentCopy) { it.copyToClipboard(context); "已复制".showToast(context) },
            SelectionAction("搜索", Icons.Outlined.Search) { },
            SelectionAction("问AI", Icons.Outlined.AutoAwesome) { },
        )
    }

    val listState = rememberLazyListState()

    LaunchedEffect(listState.canScrollForward) {
        if (!listState.canScrollForward && commentUiState.hasMore && !commentUiState.isLoadingMore) {
            onLoadMoreComments()
        }
    }

    LazyColumn(modifier = modifier.fillMaxWidth(), state = listState) {
        // 图片轮播
        if (note.images.isNotEmpty()) {
            item {
                NoteImagePager(images = note.images, onImageClick = onImageClick)
            }
        }
        // 标题
        if (!note.title.isNullOrBlank()) {
            item {
                SelectableText(
                    text = note.title,
                    actions = selectionActions,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = LimeDark,
                    ),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                )
            }
        }

        // 正文
        if (!note.content.isNullOrBlank()) {
            item {
                SelectableText(
                    text = note.content,
                    actions = selectionActions,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = LimeDark,
                        lineHeight = 22.sp,
                    ),
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
        }

        // 更新时间
        item {
            Text(
                text = formatRelativeTime(note.updateTime.orEmpty()),
                color = LimeGray,
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 16.dp, top = 10.dp, bottom = 16.dp),
            )
        }
        item { HorizontalDivider(color = LimeLightGray, thickness = 1.dp) }

        // 评论区标题栏
        item {
            CommentHeader(
                commentCount = note.commentCount,
                sort = commentUiState.sort,
                onSortChange = onSortChange,
            )
        }

        // 无评论空态
        if (!commentUiState.isLoading && commentUiState.comments.isEmpty()) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Image(
                        painter = painterResource(R.drawable.img_no_comments),
                        contentDescription = null,
                        modifier = Modifier.size(180.dp),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "这是一片荒草地", fontSize = 13.sp, color = LimeGray)
                }
            }
        }

        // 评论列表
        items(commentUiState.comments, key = { it.id }) { comment ->
            CommentCard(
                comment = comment,
                expandedReplies = commentUiState.expandedReplies[comment.id],
                onLike = { onCommentLike(comment.id) },
                onReply = onReply,
                onLoadMoreReplies = { onLoadMoreReplies(comment.id) },
                onReplyLike = { replyId -> onReplyLike(comment.id, replyId) },
            )
            HorizontalDivider(color = LimeLightGray, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp))
        }

        // 加载更多
        if (commentUiState.isLoadingMore) {
            item {
                Box(
                    modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = LimePrimary, strokeWidth = 2.dp)
                }
            }
        }

        // 到底了
        if (!commentUiState.hasMore && commentUiState.comments.isNotEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth()
                        .padding(vertical = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "- 到底了 -", fontSize = 12.sp, color = LimeGray)
                }
            }
        }

        item { Spacer(modifier = Modifier.height(72.dp)) }
    }
}
