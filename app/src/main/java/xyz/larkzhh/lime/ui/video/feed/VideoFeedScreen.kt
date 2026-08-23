package xyz.larkzhh.lime.ui.video.feed

import android.Manifest
import android.net.Uri
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Reply
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.navigation.NavHostController
import coil3.compose.AsyncImage
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import xyz.larkzhh.lime.R
import xyz.larkzhh.lime.data.network.model.CommentData
import xyz.larkzhh.lime.data.network.model.DanmakuData
import xyz.larkzhh.lime.data.network.model.NoteDetailData
import xyz.larkzhh.lime.data.network.model.ReplyData
import xyz.larkzhh.lime.navigation.Screen
import xyz.larkzhh.lime.navigation.SwipeBackScaffold
import xyz.larkzhh.lime.navigation.navigateToUserProfile
import xyz.larkzhh.lime.ui.components.CommentInputSheet
import xyz.larkzhh.lime.ui.video.components.ExpandableText
import xyz.larkzhh.lime.ui.video.components.DanmakuInputSheet
import xyz.larkzhh.lime.ui.video.components.DanmakuOverlay
import xyz.larkzhh.lime.ui.video.components.FollowButton
import xyz.larkzhh.lime.ui.video.components.VideoActionPanel
import xyz.larkzhh.lime.ui.components.GroupedBottomActionSheet
import xyz.larkzhh.lime.ui.components.GroupedSheetAction
import xyz.larkzhh.lime.ui.components.LimeAlertDialog
import xyz.larkzhh.lime.ui.components.VoiceRecordSheet
import xyz.larkzhh.lime.ui.detail.AuthorSessionHost
import xyz.larkzhh.lime.ui.detail.comment.viewmodel.CommentViewModel
import xyz.larkzhh.lime.ui.detail.comment.viewmodel.ReplyTarget
import xyz.larkzhh.lime.ui.detail.components.ImagePreviewOverlay
import xyz.larkzhh.lime.ui.detail.components.NoteBottomBar
import xyz.larkzhh.lime.ui.profile.ProfileScreen
import xyz.larkzhh.lime.ui.video.components.LikeBurst
import xyz.larkzhh.lime.ui.video.components.ScrubBar
import xyz.larkzhh.lime.ui.video.components.formatTime
import xyz.larkzhh.lime.ui.video.feed.components.CommentDrawer
import xyz.larkzhh.lime.ui.video.player.VideoPage
import xyz.larkzhh.lime.ui.video.player.VideoPlayerManager
import xyz.larkzhh.lime.util.copyToClipboard
import xyz.larkzhh.lime.util.saveVideoToGallery
import xyz.larkzhh.lime.util.showToast
import kotlin.time.Duration.Companion.milliseconds

/// 竖屏视频页
@UnstableApi
@Composable
fun VideoFeedScreen(
    navController: NavHostController,
    playerManager: VideoPlayerManager,
    onEnterMiniPlayer: (videoWidth: Int, videoHeight: Int) -> Unit,
    onExit: () -> Unit,
) {
    val viewModel: VideoFeedViewModel = hiltViewModel()
    val commentViewModel: CommentViewModel = hiltViewModel()
    val danmakuViewModel: DanmakuViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center,
    ) {
        when {
            uiState.isLoading && uiState.items.isEmpty() -> {
                CircularProgressIndicator(color = Color.White)
            }
            uiState.items.isEmpty() -> {
                Text(
                    text = uiState.error ?: "暂无视频",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 14.sp,
                )
            }
            else -> {
                VideoFeedContent(
                    navController = navController,
                    viewModel = viewModel,
                    commentViewModel = commentViewModel,
                    danmakuViewModel = danmakuViewModel,
                    playerManager = playerManager,
                    onEnterMiniPlayer = onEnterMiniPlayer,
                    onExit = onExit,
                )
            }
        }
    }
}

@UnstableApi
@kotlin.OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun VideoFeedContent(
    navController: NavHostController,
    viewModel: VideoFeedViewModel,
    commentViewModel: CommentViewModel,
    danmakuViewModel: DanmakuViewModel,
    playerManager: VideoPlayerManager,
    onEnterMiniPlayer: (videoWidth: Int, videoHeight: Int) -> Unit,
    onExit: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    val commentUiState by commentViewModel.uiState.collectAsState()
    val danmakuUiState by danmakuViewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val notificationPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS)
    } else {
        null
    }
    var justRequested by remember { mutableStateOf(false) }

    LaunchedEffect(notificationPermission?.status) {
        if (justRequested) {
            justRequested = false
            val status = notificationPermission?.status ?: return@LaunchedEffect
            if (!status.isGranted) {
                "请在设置中开启通知".showToast(context)
            }
        }
    }

    // 弹幕发送失败提示
    LaunchedEffect(danmakuUiState.sendError) {
        danmakuUiState.sendError?.let {
            it.showToast(context)
            danmakuViewModel.consumeSendError()
        }
    }

    var scrubbing by remember { mutableStateOf(false) }
    var showCommentDrawer by remember { mutableStateOf(false) } // 评论抽屉
    var showActionPanel by remember { mutableStateOf(false) }// 长按操作面板
    // 双击爱心动画
    var likeBurst by remember { mutableStateOf<Offset?>(null) }
    var likeBurstKey by remember { mutableIntStateOf(0) }
    // 评论交互
    var voiceSheetHeightDp by remember { mutableIntStateOf(0) }
    var longPressTarget by remember { mutableStateOf<LongPressTarget?>(null) }
    var pendingDeleteAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var commentPreviewImages by remember { mutableStateOf<List<String>>(emptyList()) }
    var commentPreviewIndex by remember { mutableStateOf<Int?>(null) }

    val lastIndex = (uiState.items.size - 1).coerceAtLeast(0)
    val pagerState = rememberPagerState(
        initialPage = uiState.currentIndex.coerceIn(0, lastIndex),
    ) { uiState.items.size }

    // 切页
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.collect { viewModel.onPageSettled(it) }
    }

    // 退出横屏后跳转到目标视频
    LaunchedEffect(uiState.pendingScrollTarget) {
        val t = uiState.pendingScrollTarget ?: return@LaunchedEffect
        pagerState.scrollToPage(t.coerceIn(0, (uiState.items.size - 1).coerceAtLeast(0)))
        viewModel.consumePendingScroll()
    }

    val currentPage = pagerState.currentPage.coerceIn(0, lastIndex)
    val currentItem = uiState.items.getOrNull(currentPage)
    val authorId = currentItem?.author?.id
    val selfUserId = commentViewModel.currentUserId

    // 换页重置评论
    LaunchedEffect(currentItem?.id) {
        currentItem?.id?.let { commentViewModel.init(it) }
    }

    // 换页加载弹幕
    LaunchedEffect(currentItem?.id) {
        currentItem?.id?.let { danmakuViewModel.setCurrentNote(it) }
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

    val sessionHost: AuthorSessionHost = hiltViewModel()
    val authorSession = authorId?.let { id -> remember(id) { sessionHost.ensure(id) } }// 绑定作者主页会话

    val prevEntry = navController.previousBackStackEntry
    val authorAlreadyInStack = authorId != null && when (prevEntry?.destination?.route) {
        Screen.Profile.route -> selfUserId != null && authorId == selfUserId
        Screen.UserProfile.ROUTE -> prevEntry.arguments?.getLong("userId") == authorId
        else -> false
    }// 作者主页已在返回栈，关闭左滑前进

    BackHandler(enabled = showCommentDrawer || longPressTarget != null) {
        // 优先关闭长按操作栏
        if (longPressTarget != null) {
            longPressTarget = null
        } else {
            showCommentDrawer = false
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        SwipeBackScaffold(
            backEnabled = !showCommentDrawer && navController.previousBackStackEntry != null,
            revealEntryId = { navController.previousBackStackEntry?.id },
            excludeRegion = { pos, size -> size.height > 0 && pos.y >= size.height * 0.72f },
            forwardPeek = if (showCommentDrawer || authorAlreadyInStack) null else authorId?.let { id ->
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
            VerticalPager(
                state = pagerState,
                userScrollEnabled = !scrubbing,
                beyondViewportPageCount = 1,
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black),
            ) { page ->
                val item = uiState.items[page]
                val latestItem by rememberUpdatedState(item)
                val isActive = page == pagerState.settledPage && !uiState.fullscreen
                val userPaused = item.id in uiState.pausedNoteIds

                VideoPage(
                    noteId = item.id,
                    playUrl = item.video.playUrl,
                    width = item.video.width,
                    height = item.video.height,
                    isActive = isActive,
                    userPaused = userPaused,
                    playerManager = playerManager,
                    onTogglePlay = {
                        if (!danmakuViewModel.dismissBubble()) viewModel.togglePaused(item.id)
                    },
                    title = item.title,
                    controlEnabled = !uiState.fullscreen,// 全屏时竖屏页让出播放器与画布
                    forcePaused = danmakuUiState.showInput,// 发弹幕时暂停当前视频
                    playbackSpeed = uiState.playbackSpeed,
                    autoPlayNext = uiState.autoPlayNext,
                    backgroundAudio = uiState.backgroundAudio,
                    onPlaybackEnded = {
                        // 自动连播
                        val next = page + 1
                        if (next <= lastIndex) scope.launch { pagerState.animateScrollToPage(next) }
                    },
                    gestureModifier = Modifier.pointerInput(item.id) {
                        detectTapGestures(
                            onTap = {
                                if (!danmakuViewModel.dismissBubble()) {
                                    viewModel.togglePaused(item.id)
                                }
                            },
                            onDoubleTap = { offset ->
                                danmakuViewModel.dismissBubble()
                                if (!latestItem.liked) viewModel.toggleLikeById(item.id)// 双击点赞
                                likeBurst = offset
                                likeBurstKey++
                            },
                            onLongPress = {
                                danmakuViewModel.dismissBubble()
                                showActionPanel = true
                            },
                        )
                    },
                ) { player ->
                    if (isActive) {
                        VideoChrome(
                            item = item,
                            player = player,
                            scrubbing = scrubbing,
                            clearScreen = uiState.clearScreen,
                            onScrubbingChange = { scrubbing = it },
                            onBack = { if (!navController.popBackStack()) onExit() },
                            onShare = {},
                            onAuthorClick = { navController.navigateToUserProfile(item.author.id, selfUserId) },
                            onFollow = {},
                            onToggleLike = viewModel::toggleLike,
                            onToggleFavorite = viewModel::toggleFavorite,
                            onCommentClick = { showCommentDrawer = true },
                            danmakuList = danmakuUiState.danmakuByNote[item.id].orEmpty(),
                            danmakuEnabled = danmakuUiState.enabled,
                            danmakuOpacity = uiState.danmakuOpacity,
                            currentUserId = danmakuViewModel.currentUserId,
                            pausedDanmakuId = danmakuUiState.pausedDanmakuId,
                            frozenDanmakuMs = danmakuUiState.frozenMs,
                            onDanmakuClick = { id, nowMs -> danmakuViewModel.onDanmakuClick(id, nowMs) },
                            onDismissBubble = { danmakuViewModel.dismissBubble() },
                            onSendDanmakuClick = { danmakuViewModel.openInput() },
                            onDeleteDanmaku = { danmakuViewModel.deleteDanmaku(item.id, it.id) },
                            showFullscreenButton = item.isLandscape,
                            onFullscreen = { viewModel.enterFullscreen() },
                            onEnterMiniPlayer = { onEnterMiniPlayer(item.video.width, item.video.height) },
                        )
                        // 双击点赞爱心
                        LikeBurst(position = likeBurst, triggerKey = likeBurstKey)
                    }
                }
            }
        }

        // 评论抽屉
        CommentDrawer(
            visible = showCommentDrawer,
            onDismiss = { showCommentDrawer = false },
            commentUiState = commentUiState,
            baseCommentCount = currentItem?.commentCount ?: 0,
            currentUserAvatar = commentViewModel.currentUserAvatar,
            onSortChange = commentViewModel::setSort,
            onLoadMore = { commentViewModel.loadComments() },
            onCommentLike = commentViewModel::toggleCommentLike,
            onReply = { target -> commentViewModel.openInputSheet(target) },
            onLoadMoreReplies = commentViewModel::loadMoreReplies,
            onReplyLike = commentViewModel::toggleReplyLike,
            onCommentImageClick = { images, index ->
                commentPreviewImages = images
                commentPreviewIndex = index
            },
            onCommentLongPress = { longPressTarget = LongPressTarget.Comment(it) },
            onCommentReplyLongPress = { commentId, reply -> longPressTarget = LongPressTarget.Reply(commentId, reply) },
            onAuthorClick = { userId -> navController.navigateToUserProfile(userId, selfUserId) },
            onCommentBoxClick = { commentViewModel.openInputSheet(null) },
            onVoiceClick = {
                voiceSheetHeightDp = 0
                commentViewModel.openVoiceSheet()
            },
            onAlbumClick = { navController.navigate(Screen.CommentPhotoPicker.route) },
        )

        // 评论图片预览浮层
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
        if (commentUiState.showInputSheet) {
            val hint = commentUiState.replyTarget?.let { "回复 @${it.replyToNickname}" } ?: "说点什么…"
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

        // 长按操作菜单
        val pressed = longPressTarget
        val currentUserId = commentViewModel.currentUserId// 当前登录用户
        val noteAuthorId = currentItem?.author?.id// 当前视频作者
        val menu = when (pressed) {
            is LongPressTarget.Comment -> LongPressMenu(
                replyTarget = ReplyTarget(pressed.comment.id, null, pressed.comment.author.nickname),
                copyText = pressed.comment.content,
                canDelete = currentUserId != null &&
                    (currentUserId == pressed.comment.author.id || currentUserId == noteAuthorId),
                onDelete = { pendingDeleteAction = { commentViewModel.deleteComment(pressed.comment.id) } },
            )

            is LongPressTarget.Reply -> LongPressMenu(
                replyTarget = ReplyTarget(pressed.commentId, pressed.reply.author.id, pressed.reply.author.nickname),
                copyText = pressed.reply.content,
                canDelete = currentUserId != null &&
                    (currentUserId == pressed.reply.author.id || currentUserId == noteAuthorId),
                onDelete = { pendingDeleteAction = { commentViewModel.deleteReply(pressed.commentId, pressed.reply.id) } },
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
                        label = "回复",
                        icon = Icons.AutoMirrored.Outlined.Reply,
                        onClick = { commentViewModel.openInputSheet(m.replyTarget) },
                    ),
                    GroupedSheetAction(
                        label = "复制",
                        icon = Icons.Outlined.ContentCopy,
                        iconSize = 20.dp,
                        onClick = {
                            m.copyText?.copyToClipboard(context)
                            "已复制".showToast(context)
                        },
                    ),
                ))
                if (m.canDelete) {
                    add(listOf(
                        GroupedSheetAction(
                            label = "删除",
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
                title = "确认删除这条评论吗？",
                onDismissRequest = { pendingDeleteAction = null },
                onFirstButtonClick = { pendingDeleteAction = null },
                onSecondButtonClick = {
                    pendingDeleteAction?.invoke()
                    pendingDeleteAction = null
                },
            )
        }

        // 弹幕输入框
        if (danmakuUiState.showInput) {
            DanmakuInputSheet(
                color = danmakuUiState.color,
                onColorChange = danmakuViewModel::setColor,
                onToggleOff = {
                    danmakuViewModel.toggleEnabled()// 关闭弹幕
                    danmakuViewModel.closeInput()
                    "弹幕已关闭".showToast(context)
                },
                onSend = { text ->
                    val item = currentItem ?: return@DanmakuInputSheet
                    val pos = playerManager.currentPositionOf(item.id)
                    danmakuViewModel.sendDanmaku(item.id, text, pos)
                    danmakuViewModel.closeInput()
                },
                onDismiss = danmakuViewModel::closeInput,
            )
        }

        // 长按操作面板
        VideoActionPanel(
            visible = showActionPanel,
            onDismiss = { showActionPanel = false },
            currentSpeed = uiState.playbackSpeed,
            onSpeedChange = {
                viewModel.setPlaybackSpeed(it)
                showActionPanel = false
                "已切换 ${it}倍速".showToast(context)
            },
            danmakuEnabled = danmakuUiState.enabled,
            onToggleDanmaku = {
                val wasEnabled = danmakuUiState.enabled
                danmakuViewModel.toggleEnabled()
                if (wasEnabled) "弹幕已关闭".showToast(context) else "弹幕已开启".showToast(context)
            },
            danmakuOpacity = uiState.danmakuOpacity,
            onOpacityChange = viewModel::setDanmakuOpacity,
            autoPlayNext = uiState.autoPlayNext,
            onToggleAutoPlayNext = {
                val wasOn = uiState.autoPlayNext
                viewModel.toggleAutoPlayNext()
                if (wasOn) "自动连播已关闭".showToast(context) else "自动连播已开启".showToast(context)
            },
            backgroundAudio = uiState.backgroundAudio,
            onToggleBackgroundAudio = {
                val wasOn = uiState.backgroundAudio
                viewModel.toggleBackgroundAudio()
                if (!wasOn && notificationPermission != null && !notificationPermission.status.isGranted) {
                    justRequested = true
                    notificationPermission.launchPermissionRequest()
                }
                if (wasOn) "后台播放已关闭".showToast(context) else "后台播放已开启".showToast(context)
            },
            onClearScreen = { viewModel.toggleClearScreen() },
            clearScreen = uiState.clearScreen,
            onSaveVideo = {
                val url = currentItem?.video?.playUrl ?: return@VideoActionPanel
                "开始保存…".showToast(context)
                scope.launch {
                    val ok = saveVideoToGallery(context, url)
                    (if (ok) "已保存到相册" else "保存失败").showToast(context)
                }
            },
        )

        // 横屏全屏
        if (uiState.fullscreen) {
            LandscapeFullscreenHost(
                viewModel = viewModel,
                danmakuViewModel = danmakuViewModel,
                playerManager = playerManager,
                onExit = viewModel::exitFullscreen,
            )
        }
    }
}

// 长按目标
private sealed interface LongPressTarget {
    data class Comment(val comment: CommentData) : LongPressTarget
    data class Reply(val commentId: Long, val reply: ReplyData) : LongPressTarget
}

/// 长按菜单动作
private data class LongPressMenu(
    val replyTarget: ReplyTarget,
    val copyText: String?,
    val canDelete: Boolean,
    val onDelete: () -> Unit,
)

/// 视频页 chrome 浮层
@OptIn(UnstableApi::class)
@Composable
private fun VideoChrome(
    item: VideoItem,
    player: ExoPlayer?,
    scrubbing: Boolean,
    clearScreen: Boolean = false,
    onScrubbingChange: (Boolean) -> Unit,
    onBack: () -> Unit,
    onShare: () -> Unit,
    onAuthorClick: () -> Unit,
    onFollow: () -> Unit,
    onToggleLike: () -> Unit,
    onToggleFavorite: () -> Unit,
    onCommentClick: () -> Unit,
    danmakuList: List<DanmakuData> = emptyList(),
    danmakuEnabled: Boolean = true,
    danmakuOpacity: Float = 1f,
    currentUserId: Long? = null,
    pausedDanmakuId: Long? = null,
    frozenDanmakuMs: Long = 0L,
    onDanmakuClick: (danmakuId: Long, nowMs: Long) -> Unit = { _, _ -> },
    onDismissBubble: () -> Unit = {},
    onSendDanmakuClick: () -> Unit = {},
    onDeleteDanmaku: (DanmakuData) -> Unit = {},
    showFullscreenButton: Boolean = false,
    onFullscreen: () -> Unit = {},// 进入横屏全屏
    onEnterMiniPlayer: () -> Unit = {},
) {
    // 播放进度轮询
    var positionMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(item.video.durationMs.coerceAtLeast(1L)) }
    LaunchedEffect(player) {
        while (true) {
            player?.let {
                positionMs = it.currentPosition
                val d = it.duration
                if (d > 0) durationMs = d
            }
            delay(200.milliseconds)
        }
    }
    val fraction = if (durationMs > 0) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f

    Box(modifier = Modifier.fillMaxSize()) {
        // 顶部栏与弹幕区
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .statusBarsPadding(),
        ) {
            // 顶部控制栏
            if (!scrubbing && !clearScreen) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "返回",
                        tint = Color.White,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() },
                                onClick = onBack,
                            )
                            .padding(8.dp),
                    )
                    // 小窗播放
                    Icon(
                        painter = painterResource(R.drawable.ic_pip),
                        contentDescription = "小窗播放",
                        tint = Color.White,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() },
                                onClick = onEnterMiniPlayer,
                            )
                            .padding(8.dp),
                    )
                    Spacer(Modifier.weight(1f))
                    Icon(
                        imageVector = Icons.Filled.Share,
                        contentDescription = "分享",
                        tint = Color.White,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() },
                                onClick = onShare,
                            )
                            .padding(8.dp),
                    )
                }
            } else if (!scrubbing) {
                // 清屏时占住顶部栏高度，弹幕不随之上移
                Spacer(Modifier.height(52.dp))
            }
            // 弹幕区
            if (!scrubbing) {
                DanmakuOverlay(
                    danmakuList = danmakuList,
                    player = player,
                    enabled = danmakuEnabled,
                    currentUserId = currentUserId,
                    noteAuthorId = item.author.id,
                    pausedDanmakuId = pausedDanmakuId,
                    frozenMs = frozenDanmakuMs,
                    opacity = danmakuOpacity,
                    onDanmakuClick = onDanmakuClick,
                    onDismissBubble = onDismissBubble,
                    onDelete = onDeleteDanmaku,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                )
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
        ) {
            if (!scrubbing && !clearScreen) {
                Column(modifier = Modifier.padding(horizontal = 12.dp)) {
                    // 全屏观看入口
                    if (showFullscreenButton) {
                        Row(
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .shadow(
                                    elevation = 8.dp,
                                    shape = RoundedCornerShape(50),
                                    ambientColor = Color.Black,
                                    spotColor = Color.Black,
                                )
                                .clip(RoundedCornerShape(50))
                                .background(Color.White.copy(alpha = 0.08f))
                                .border(
                                    width = 0.5.dp,
                                    color = Color.White.copy(alpha = 0.25f),
                                    shape = RoundedCornerShape(50),
                                )
                                .clickable(
                                    indication = null,
                                    interactionSource = remember { MutableInteractionSource() },
                                    onClick = { onFullscreen() },
                                )
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Fullscreen,
                                contentDescription = "全屏观看",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp),
                            )
                            Text(
                                text = "全屏观看",
                                color = Color.White,
                                fontSize = 13.sp,
                            )
                        }
                        Spacer(Modifier.height(16.dp))
                    }
                    // 作者头像、昵称、关注
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        AsyncImage(
                            model = item.author.avatar,
                            contentDescription = item.author.nickname,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.2f))
                                .clickable(
                                    indication = null,
                                    interactionSource = remember { MutableInteractionSource() },
                                    onClick = onAuthorClick,
                                ),
                        )
                        Text(
                            text = item.author.nickname,
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .clickable(
                                    indication = null,
                                    interactionSource = remember { MutableInteractionSource() },
                                    onClick = onAuthorClick,
                                ),
                        )
                        FollowButton(followed = false, onClick = onFollow)
                        Spacer(Modifier.weight(1f))
                        // 发弹幕
                        Icon(
                            painter = painterResource(R.drawable.ic_barrage),
                            contentDescription = "发弹幕",
                            tint = Color.White,
                            modifier = Modifier
                                .size(20.dp)
                                .clickable(
                                    indication = null,
                                    interactionSource = remember { MutableInteractionSource() },
                                    onClick = onSendDanmakuClick,
                                ),
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    // 可展开标题与正文
                    ExpandableText(title = item.title, body = item.body)
                    Spacer(Modifier.height(10.dp))
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
            ) {
                // 进度条
                if (scrubbing) {
                    Text(
                        text = "${formatTime(positionMs)} / ${formatTime(durationMs)}",
                        color = Color.White,
                        fontSize = 13.sp,
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(bottom = 6.dp),
                    )
                }
                ScrubBar(
                    fraction = fraction,
                    onDragStart = { onScrubbingChange(true) },
                    onSeek = { v -> player?.seekTo((v * durationMs).toLong()) },
                    onDragEnd = { v ->
                        player?.seekTo((v * durationMs).toLong())
                        onScrubbingChange(false)
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            // 底部栏
            NoteBottomBar(
                note = item.toNoteDetailData(),
                onToggleLike = onToggleLike,
                onToggleFavorite = onToggleFavorite,
                onCommentClick = onCommentClick,
                containerColor = Color.Transparent,
                contentColor = Color.White,
                inputBackground = Color.White.copy(alpha = 0.18f),
                elevated = false,
                compact = true,
                modifier = Modifier.alpha(if (scrubbing) 0f else 1f),
            )
        }
    }
}

private fun VideoItem.toNoteDetailData(): NoteDetailData = NoteDetailData(
    id = id,
    title = title,
    content = body,
    status = 1,
    images = emptyList(),
    likeCount = likeCount,
    favCount = favCount,
    viewCount = 0,
    commentCount = commentCount,
    liked = liked,
    favorited = favorited,
    author = author,
    noteType = 2,
    video = video,
)
