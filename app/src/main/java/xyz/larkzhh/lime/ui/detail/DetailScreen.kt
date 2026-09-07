package xyz.larkzhh.lime.ui.detail

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import xyz.larkzhh.lime.R
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Reply
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.graphics.Color
import xyz.larkzhh.lime.data.network.model.CommentData
import xyz.larkzhh.lime.data.network.model.NoteDetailData
import xyz.larkzhh.lime.data.network.model.ReplyData
import xyz.larkzhh.lime.domain.model.ChatNote
import xyz.larkzhh.lime.domain.model.FollowActionState
import xyz.larkzhh.lime.domain.model.toFollowActionState
import xyz.larkzhh.lime.ui.auth.LoginGate
import xyz.larkzhh.lime.navigation.state.PendingNoteEdit
import xyz.larkzhh.lime.navigation.route.Screen
import xyz.larkzhh.lime.navigation.component.SwipeBackScaffold
import xyz.larkzhh.lime.navigation.route.navigateToUserProfile
import xyz.larkzhh.lime.navigation.state.PendingChatStore
import xyz.larkzhh.lime.ui.components.CommentInputSheet
import xyz.larkzhh.lime.ui.components.ErrorState
import xyz.larkzhh.lime.ui.components.GroupedBottomActionSheet
import xyz.larkzhh.lime.ui.components.GroupedSheetAction
import xyz.larkzhh.lime.ui.components.LimeAlertDialog
import xyz.larkzhh.lime.ui.components.LoadMoreErrorItem
import xyz.larkzhh.lime.ui.components.NoteManageSheet
import xyz.larkzhh.lime.ui.components.SelectableText
import xyz.larkzhh.lime.ui.components.SelectionAction
import xyz.larkzhh.lime.ui.components.UnfollowConfirmDialog
import xyz.larkzhh.lime.ui.components.VoiceRecordSheet
import xyz.larkzhh.lime.ui.detail.components.AuthorBar
import xyz.larkzhh.lime.ui.detail.comment.components.CommentCard
import xyz.larkzhh.lime.ui.detail.comment.components.CommentHeader
import xyz.larkzhh.lime.ui.detail.comment.components.CommentInputBar
import xyz.larkzhh.lime.ui.detail.comment.viewmodel.CommentSort
import xyz.larkzhh.lime.ui.detail.comment.viewmodel.CommentUiState
import xyz.larkzhh.lime.ui.detail.comment.viewmodel.CommentViewModel
import xyz.larkzhh.lime.ui.detail.comment.viewmodel.ReplyTarget
import xyz.larkzhh.lime.ui.detail.components.ImagePreviewOverlay
import xyz.larkzhh.lime.ui.detail.components.NoteBottomBar
import xyz.larkzhh.lime.ui.detail.components.NoteImagePager
import xyz.larkzhh.lime.ui.detail.translate.FullTextUiState
import xyz.larkzhh.lime.ui.detail.translate.TranslateResultSheet
import xyz.larkzhh.lime.ui.detail.translate.TranslateViewModel
import xyz.larkzhh.lime.ui.profile.ProfileScreen
import xyz.larkzhh.lime.ui.theme.LimeGray
import xyz.larkzhh.lime.util.copyToClipboard
import xyz.larkzhh.lime.util.text.formatRelativeTime
import xyz.larkzhh.lime.util.generateGradientQrBitmap
import xyz.larkzhh.lime.util.limeNoteQrContent
import xyz.larkzhh.lime.util.media.saveBitmapToGallery
import xyz.larkzhh.lime.util.showToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// 长按目标
private sealed interface LongPressTarget {
    data class Comment(val comment: CommentData) : LongPressTarget
    data class Reply(val commentId: Long, val reply: ReplyData) : LongPressTarget
}

/// 长按菜单动作列表
private data class LongPressMenu(
    val replyTarget: ReplyTarget,
    val copyText: String?,
    val canDelete: Boolean,
    val onDelete: () -> Unit,
)

@Composable
fun DetailScreen(
    navController: NavHostController,
    noteId: String,
    viewModel: DetailViewModel = hiltViewModel(),
    commentViewModel: CommentViewModel = hiltViewModel(),
    translateViewModel: TranslateViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val commentUiState by commentViewModel.uiState.collectAsState()
    val translateUiState by translateViewModel.uiState.collectAsState()
    val fullTextUiState by translateViewModel.fullText.collectAsState()
    val relations by viewModel.relations.collectAsState()

    // 评论图片预览本地状态
    var commentPreviewImages by remember { mutableStateOf<List<String>>(emptyList()) }
    var commentPreviewIndex by remember { mutableStateOf<Int?>(null) }
    var longPressTarget by remember { mutableStateOf<LongPressTarget?>(null) }
    var pendingDeleteAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var showUnfollowConfirm by remember { mutableStateOf(false) }
    var showNoteManage by remember { mutableStateOf(false) }
    var showDeleteNoteConfirm by remember { mutableStateOf(false) }
    var showQrSaveConfirm by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val t = detailTexts()

    LaunchedEffect(noteId) {
        val id = noteId.toLongOrNull() ?: return@LaunchedEffect
        viewModel.loadNote(id)
        commentViewModel.init(id)
    }

    // 观察从图片选择页面返回的图片
    LaunchedEffect(Unit) {
        val savedStateHandle = navController.currentBackStackEntry?.savedStateHandle ?: return@LaunchedEffect
        savedStateHandle.getStateFlow<List<Uri>?>("comment_images", null).collect { uris ->
            if (!uris.isNullOrEmpty()) {
                commentViewModel.addCommentImages(uris)
                savedStateHandle.remove<List<Uri>>("comment_images")
            }
        }
    }

    val authorId = uiState.note?.author?.id
    val selfUserId = commentViewModel.currentUserId
    // 作者关注状态
    val authorFollowState = authorId?.let { id ->
        if (id == selfUserId) null else relations[id]?.toFollowActionState() ?: FollowActionState.Follow
    }
    val sessionHost: AuthorSessionHost = hiltViewModel()
    val authorSession = authorId?.let { id -> remember(id) { sessionHost.ensure(id) } }// 绑定作者主页会话
    val prevEntry = navController.previousBackStackEntry// 作者主页为详情页的上一页，关闭左滑前进预览
    val authorAlreadyInStack = authorId != null && when (prevEntry?.destination?.route) {
        Screen.Profile.route -> selfUserId != null && authorId == selfUserId
        Screen.UserProfile.ROUTE -> prevEntry.arguments?.getLong("userId") == authorId
        else -> false
    }

    SwipeBackScaffold(
        backEnabled = navController.previousBackStackEntry != null,
        revealEntryId = { navController.previousBackStackEntry?.id },
        forwardPeek = if (authorAlreadyInStack) null else authorId?.let { id ->
            authorSession?.let { session ->
                {
                    ProfileScreen(
                        navController = navController,
                        userId = id,
                        viewModel = session.profileViewModel,
                        notesViewModel = session.notesViewModel,
                        session = session,
                    )
                }
            }
        },
        onCommitForward = {
            if (authorId != null) {
                navController.navigateToUserProfile(authorId, selfUserId, suppressEnterAnimation = true)
            }
        },
    ) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            when {
                uiState.isLoading && uiState.note == null -> {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }

                uiState.error != null && uiState.note == null -> {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        ErrorState(message = uiState.error, onRetry = viewModel::retry)
                    }
                }

                uiState.note != null -> {
                    AuthorBar(
                        note = uiState.note!!,
                        onBack = { navController.popBackStack() },
                        onAuthorClick = {
                            navController.navigateToUserProfile(uiState.note!!.author.id, selfUserId)
                        },
                        followState = authorFollowState,
                        onFollowClick = {
                            if (authorFollowState == FollowActionState.Follow) {
                                viewModel.followAuthor()
                            } else {
                                showUnfollowConfirm = true
                            }
                        },
                        onShareClick = { showQrSaveConfirm = true },
                    )
                    NoteContent(
                        note = uiState.note!!,
                        commentUiState = commentUiState,
                        modifier = Modifier.weight(1f),
                        onImageClick = viewModel::showImagePreview,
                        onSortChange = commentViewModel::setSort,
                        onRetryComments = commentViewModel::retryComments,
                        onLoadMoreComments = { commentViewModel.loadComments() },
                        onCommentLike = commentViewModel::toggleCommentLike,
                        onReply = { replyId ->
                            if (!LoginGate.onRequireLogin(null)) commentViewModel.openInputSheet(replyId)
                        },
                        onLoadMoreReplies = commentViewModel::loadMoreReplies,
                        onReplyLike = commentViewModel::toggleReplyLike,
                        onCommentImageClick = { images, index ->
                            commentPreviewImages = images
                            commentPreviewIndex = index
                        },
                        onCommentLongPress = { longPressTarget = LongPressTarget.Comment(it) },
                        onCommentReplyLongPress = { commentId, reply -> longPressTarget = LongPressTarget.Reply(commentId, reply) },
                        currentUserAvatar = commentViewModel.currentUserAvatar,
                        onCommentBoxClick = {
                            if (!LoginGate.onRequireLogin(null)) commentViewModel.openInputSheet(null)
                        },
                        onVoiceClick = {
                            if (!LoginGate.onRequireLogin(null)) {
                                commentViewModel.openInputSheet(null)
                                if (commentUiState.pendingImages.isNotEmpty()) {
                                    t.imageVoiceMutexToast.showToast(context)
                                } else if (commentUiState.pendingVoice != null) {
                                    t.voiceOnlyToast.showToast(context)
                                } else {
                                    commentViewModel.openVoiceSheet()
                                }
                            }
                        },
                        onAlbumClick = {
                            if (!LoginGate.onRequireLogin(null)) {
                                commentViewModel.openInputSheet(null)
                                if (commentUiState.pendingVoice != null) {
                                    t.imageVoiceMutexToast.showToast(context)
                                } else {
                                    navController.navigate(Screen.CommentPhotoPicker.route)
                                }
                            }
                        },
                        onAuthorClick = { userId ->
                            navController.navigateToUserProfile(userId, selfUserId)
                        },
                        onTranslate = translateViewModel::translate,
                        onSearch = { text ->
                            navController.navigate(Screen.Search.createRoute(text))
                        },
                        onAskAi = { text ->
                            if (!LoginGate.onRequireLogin(null)) {
                                val n = uiState.note
                                PendingChatStore.askAiNote =
                                    n?.let { ChatNote(it.id, it.title, it.images.firstOrNull()?.url) }
                                PendingChatStore.askAiText = text
                                navController.navigate(Screen.AiChat.createRoute(Screen.AiChat.NEW_CONVERSATION))
                            }
                        },
                        fullText = fullTextUiState,
                        onToggleFullText = {
                            translateViewModel.toggleFullTextTranslation(
                                uiState.note?.title,
                                uiState.note?.content,
                            )
                        },
                    )
                    NoteBottomBar(
                        note = uiState.note!!.copy(
                            commentCount = uiState.note!!.commentCount + commentUiState.commentCountDelta
                        ),
                        onToggleLike = viewModel::toggleLike,
                        onToggleFavorite = viewModel::toggleFavorite,
                        onCommentClick = {
                            if (!LoginGate.onRequireLogin(null)) commentViewModel.openInputSheet(null)
                        },
                        isAuthor = selfUserId != null && uiState.note!!.author.id == selfUserId,
                        onManageClick = { showNoteManage = true },
                    )
                }
            }
        }

        // 笔记图片全屏预览浮层
        val previewIndex = uiState.previewImageIndex
        val previewNote = uiState.note
        if (previewIndex != null && previewNote != null) {
            Dialog(
                onDismissRequest = viewModel::hideImagePreview,
                properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
            ) {
                ImagePreviewOverlay(
                    images = previewNote.images.map { it.url },
                    initialIndex = previewIndex,
                    onDismiss = viewModel::hideImagePreview,
                )
            }
        }

        // 评论图片全屏预览浮层
        val commentIdx = commentPreviewIndex
        if (commentIdx != null && commentPreviewImages.isNotEmpty()) {
            Dialog(
                onDismissRequest = { commentPreviewIndex = null },
                properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
            ) {
                ImagePreviewOverlay(
                    images = commentPreviewImages,
                    initialIndex = commentIdx,
                    onDismiss = { commentPreviewIndex = null },
                )
            }
        }

        // 评论输入框
        var voiceSheetHeightDp by remember { mutableIntStateOf(0) }
        if (commentUiState.showInputSheet) {
            val hint = commentUiState.replyTarget?.let { target ->
                stringResource(R.string.comment_reply_to_hint, target.replyToNickname)
            } ?: stringResource(R.string.comment_input_hint)
            CommentInputSheet(
                hint = hint,
                isSubmitting = commentUiState.isSubmitting,
                selectedImages = commentUiState.pendingImages,
                pendingVoice = commentUiState.pendingVoice,
                onImagePickRequest = {
                    navController.navigate(Screen.CommentPhotoPicker.route)
                },
                onRemoveImage = commentViewModel::removeCommentImage,
                onVoiceRecordRequest = { heightDp ->
                    voiceSheetHeightDp = heightDp
                    commentViewModel.openVoiceSheet()
                },
                onRemoveVoice = commentViewModel::removePendingVoice,
                onSubmit = commentViewModel::submitComment,
                onDismiss = commentViewModel::closeInputSheet,
            )
        }

        // 录音面板
        if (commentUiState.showVoiceSheet) {
            VoiceRecordSheet(
                sheetTotalHeightDp = voiceSheetHeightDp,
                onVoiceRecorded = commentViewModel::setPendingVoice,
                onDismiss = commentViewModel::closeVoiceSheet,
            )
        }

        // 选词翻译译文面板
        if (translateUiState.visible) {
            TranslateResultSheet(
                state = translateUiState,
                onDismiss = translateViewModel::dismiss,
                onRetry = translateViewModel::retry,
                onBackgroundDownload = {
                    translateViewModel.scheduleBackgroundDownload()
                    translateViewModel.dismiss()
                    t.bgDownloadAddedToast.showToast(context)
                },
                onSwitchDirection = translateViewModel::switchDirection,
                onCopy = { text ->
                    text.copyToClipboard(context)
                    t.copiedToast.showToast(context)
                },
            )
        }

        // 长按操作菜单
        val pressed = longPressTarget
        val currentUserId = commentViewModel.currentUserId// 当前登录用户
        val noteAuthorId = uiState.note?.author?.id// 笔记作者
        // 菜单动作列表参数
        val menu = when (pressed) {
            is LongPressTarget.Comment -> LongPressMenu(
                replyTarget = ReplyTarget(
                    pressed.comment.id,
                    null,
                    pressed.comment.author.nickname
                ),
                copyText = pressed.comment.content,
                canDelete = currentUserId != null &&
                        (currentUserId == pressed.comment.author.id || currentUserId == noteAuthorId),
                onDelete = {
                    pendingDeleteAction = { commentViewModel.deleteComment(pressed.comment.id) }
                },
            )

            is LongPressTarget.Reply -> LongPressMenu(
                replyTarget = ReplyTarget(
                    pressed.commentId,
                    pressed.reply.author.id,
                    pressed.reply.author.nickname
                ),
                copyText = pressed.reply.content,
                canDelete = currentUserId != null &&
                        (currentUserId == pressed.reply.author.id || currentUserId == noteAuthorId),
                onDelete = {
                    pendingDeleteAction =
                        { commentViewModel.deleteReply(pressed.commentId, pressed.reply.id) }
                },
            )

            null -> null// 未长按
        }
        GroupedBottomActionSheet(
            visible = menu != null,
            onDismiss = { longPressTarget = null },
            groups = buildList {
                val m = menu ?: return@buildList
                add(listOf(
                    GroupedSheetAction(
                        label = t.replyMenuLabel,
                        icon = Icons.AutoMirrored.Outlined.Reply,
                        onClick = { commentViewModel.openInputSheet(m.replyTarget) },
                    ),
                    GroupedSheetAction(
                        label = t.copyMenuLabel,
                        icon = Icons.Outlined.ContentCopy,
                        iconSize = 20.dp,
                        onClick = {
                            m.copyText?.copyToClipboard(context)
                            t.copiedToast.showToast(context)
                        },
                    ),
                    GroupedSheetAction(
                        label = t.translateMenuLabel,
                        icon = Icons.Outlined.Translate,
                        iconSize = 20.dp,
                        onClick = {
                            m.copyText?.let { translateViewModel.translate(it) }
                        },
                    ),
                ))
                if (m.canDelete) {
                    add(listOf(
                        GroupedSheetAction(
                            label = t.deleteMenuLabel,
                            icon = Icons.Outlined.Delete,
                            textColor = Color(0xFFFF3B30),
                            onClick = m.onDelete,
                        ),
                    ))
                }
            },
        )

        // 删除确认对话框
        if (pendingDeleteAction != null) {
            LimeAlertDialog(
                title = stringResource(R.string.comment_delete_confirm_title),
                firstButtonText = stringResource(R.string.cancel),
                secondButtonText = stringResource(R.string.delete),
                secondButtonColor = Color(0xFFFF3B30),
                onDismissRequest = { pendingDeleteAction = null },
                onFirstButtonClick = { pendingDeleteAction = null },
                onSecondButtonClick = {
                    pendingDeleteAction?.invoke()
                    pendingDeleteAction = null
                },
            )
        }

        // 笔记管理菜单
        NoteManageSheet(
            visible = showNoteManage,
            onDismiss = { showNoteManage = false },
            onEdit = {
                uiState.note?.let { note ->
                    PendingNoteEdit.noteId = note.id
                    PendingNoteEdit.isVideo = false
                    navController.navigate(Screen.NotePublish.route)
                }
            },
            onDelete = {
                showDeleteNoteConfirm = true
            },
        )

        // 删除笔记确认
        if (showDeleteNoteConfirm) {
            LimeAlertDialog(
                title = stringResource(R.string.detail_note_delete_confirm_title),
                firstButtonText = stringResource(R.string.cancel),
                secondButtonText = stringResource(R.string.delete),
                secondButtonColor = Color(0xFFFF3B30),
                onDismissRequest = { showDeleteNoteConfirm = false },
                onFirstButtonClick = { showDeleteNoteConfirm = false },
                onSecondButtonClick = {
                    showDeleteNoteConfirm = false
                    viewModel.deleteCurrentNote { ok ->
                        if (ok) {
                            t.noteDeletedToast.showToast(context)
                            navController.popBackStack()
                        } else {
                            t.deleteFailedToast.showToast(context)
                        }
                    }
                },
            )
        }

        // 生成笔记二维码
        if (showQrSaveConfirm) {
            LimeAlertDialog(
                title = stringResource(R.string.detail_save_qr_title),
                text = stringResource(R.string.detail_save_qr_message),
                firstButtonText = stringResource(R.string.cancel),
                secondButtonText = stringResource(R.string.save),
                onDismissRequest = { showQrSaveConfirm = false },
                onFirstButtonClick = { showQrSaveConfirm = false },
                onSecondButtonClick = {
                    showQrSaveConfirm = false
                    uiState.note?.let { note ->
                        scope.launch {
                            val qr = withContext(Dispatchers.IO) {
                                generateGradientQrBitmap(limeNoteQrContent(note.id))
                            }
                            if (qr != null) {
                                val ok = saveBitmapToGallery(context, qr, "lime_note_${note.id}.jpg")
                                if (ok) t.qrSavedToast.showToast(context)
                                else t.saveFailedToast.showToast(context)
                            } else {
                                t.qrGenFailedToast.showToast(context)
                            }
                        }
                    }
                },
            )
        }

        // 取消关注确认对话框
        if (showUnfollowConfirm) {
            UnfollowConfirmDialog(
                onCancel = { showUnfollowConfirm = false },
                onConfirm = {
                    viewModel.unfollowAuthor()
                    showUnfollowConfirm = false
                },
            )
        }
    }
    }
}

@Composable
private fun NoteContent(
    note: NoteDetailData,
    commentUiState: CommentUiState,
    onImageClick: (Int) -> Unit,
    onSortChange: (CommentSort) -> Unit,
    onRetryComments: () -> Unit,
    onLoadMoreComments: () -> Unit,
    onCommentLike: (Long) -> Unit,
    onReply: (ReplyTarget) -> Unit,
    onLoadMoreReplies: (Long) -> Unit,
    onReplyLike: (commentId: Long, replyId: Long) -> Unit,
    onCommentImageClick: (images: List<String>, index: Int) -> Unit,
    onCommentLongPress: (CommentData) -> Unit,
    onCommentReplyLongPress: (commentId: Long, reply: ReplyData) -> Unit,
    currentUserAvatar: String?,
    onCommentBoxClick: () -> Unit,
    onVoiceClick: () -> Unit,
    onAlbumClick: () -> Unit,
    onAuthorClick: (Long) -> Unit,
    onTranslate: (String) -> Unit,
    onSearch: (String) -> Unit,
    onAskAi: (String) -> Unit,
    fullText: FullTextUiState,
    onToggleFullText: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val chatActionPainter = painterResource(R.drawable.ic_chat)
    // 本地化文案（长按菜单与 Toast 使用）
    val copyActionText = stringResource(R.string.chat_copy)
    val searchActionText = stringResource(R.string.home_search_cd)
    val translateActionText = stringResource(R.string.drawer_translate)
    val askAiActionText = stringResource(R.string.shortcut_ai)
    val copiedToastText = stringResource(R.string.detail_copied)
    val translateFailedToast = stringResource(R.string.detail_translate_failed)
    val selectionActions = remember(
        context,
        copyActionText,
        searchActionText,
        translateActionText,
        askAiActionText,
        copiedToastText,
        onTranslate,
        onSearch,
        onAskAi,
    ) {
        listOf(
            SelectionAction(
                copyActionText,
                Icons.Outlined.ContentCopy
            ) { it.copyToClipboard(context); copiedToastText.showToast(context) },
            SelectionAction(searchActionText, Icons.Outlined.Search) { onSearch(it) },
            SelectionAction(translateActionText, Icons.Outlined.Translate) { onTranslate(it) },
            SelectionAction(askAiActionText, painter = chatActionPainter) { onAskAi(it) },
        )
    }

    val listState = rememberLazyListState()

    // 评论语音播放互斥
    var playingVoiceId by remember { mutableStateOf<Long?>(null) }

    // 翻译失败提示
    LaunchedEffect(fullText.error) {
        if (fullText.error) {
            translateFailedToast.showToast(context)
        }
    }

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
        val displayTitle = if (fullText.translated) fullText.translatedTitle ?: note.title else note.title
        if (!displayTitle.isNullOrBlank()) {
            item {
                SelectableText(
                    text = displayTitle,
                    actions = selectionActions,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    ),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                )
            }
        }

        // 正文
        val displayContent = if (fullText.translated) fullText.translatedContent ?: note.content else note.content
        if (!displayContent.isNullOrBlank()) {
            item {
                SelectableText(
                    text = displayContent,
                    actions = selectionActions,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 22.sp,
                    ),
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
        }

        // 更新时间、一键翻译
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = formatRelativeTime(note.updateTime.orEmpty()),
                    color = LimeGray,
                    fontSize = 12.sp,
                    modifier = Modifier.weight(1f),
                )
                if (fullText.translating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() },
                            ) { onToggleFullText() }
                            .padding(horizontal = 6.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Translate,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(13.dp),
                        )
                        Text(
                            text = if (fullText.translated) stringResource(R.string.detail_view_original) else stringResource(R.string.detail_translate_full),
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 12.sp,
                        )
                    }
                }
            }
        }
        item { HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f), thickness = 1.dp) }

        // 评论区标题栏
        item {
            CommentHeader(
                commentCount = note.commentCount + commentUiState.commentCountDelta,
                sort = commentUiState.sort,
                onSortChange = onSortChange,
            )
        }

        // 评论输入栏
        item {
            CommentInputBar(
                currentUserAvatar = currentUserAvatar,
                onCommentClick = onCommentBoxClick,
                onVoiceClick = onVoiceClick,
                onAlbumClick = onAlbumClick,
            )
        }

        // 评论加载失败重试
        if (commentUiState.error != null && commentUiState.comments.isEmpty() && !commentUiState.isLoading) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    ErrorState(message = commentUiState.error, onRetry = onRetryComments)
                }
            }
        }

        // 无评论空态
        if (!commentUiState.isLoading && commentUiState.error == null && commentUiState.comments.isEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Image(
                        painter = painterResource(R.drawable.img_no_comments),
                        contentDescription = null,
                        modifier = Modifier.size(180.dp),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = stringResource(R.string.comment_empty_hint), fontSize = 13.sp, color = LimeGray)
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
                onImageClick = onCommentImageClick,
                playingVoiceId = playingVoiceId,
                onVoicePlay = { id -> playingVoiceId = id },
                onVoiceStop = { playingVoiceId = null },
                onLongPress = { onCommentLongPress(comment) },
                onReplyLongPress = { reply -> onCommentReplyLongPress(comment.id, reply) },
                onAuthorClick = onAuthorClick,
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f), thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp))
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
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.primary, strokeWidth = 2.dp)
                }
            }
        }

        // 加载更多评论失败重试
        if (commentUiState.error != null && commentUiState.comments.isNotEmpty() && !commentUiState.isLoadingMore) {
            item {
                LoadMoreErrorItem(message = commentUiState.error, onRetry = onRetryComments)
            }
        }

        // 到底了
        if (!commentUiState.hasMore && commentUiState.comments.isNotEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = stringResource(R.string.detail_end_of_list), fontSize = 12.sp, color = LimeGray)
                }
            }
        }

        item { Spacer(modifier = Modifier.height(72.dp)) }
    }
}
