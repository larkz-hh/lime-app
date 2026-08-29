package xyz.larkzhh.lime.ui.ai

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import kotlinx.coroutines.launch
import xyz.larkzhh.lime.domain.model.ChatConversation
import xyz.larkzhh.lime.domain.model.ChatMessage
import xyz.larkzhh.lime.domain.model.ChatMessageStatus
import xyz.larkzhh.lime.domain.model.ChatRole
import xyz.larkzhh.lime.ui.components.BottomActionSheet
import xyz.larkzhh.lime.ui.components.LimeAlertDialog
import xyz.larkzhh.lime.ui.components.SheetAction
import xyz.larkzhh.lime.ui.components.chat.ChatAddSheet
import xyz.larkzhh.lime.ui.components.chat.ChatBubbleData
import xyz.larkzhh.lime.ui.components.chat.ChatBubbleStatus
import xyz.larkzhh.lime.ui.components.chat.ChatInputBar
import xyz.larkzhh.lime.ui.components.chat.ChatInputImage
import xyz.larkzhh.lime.ui.components.chat.ChatInputImageState
import xyz.larkzhh.lime.ui.components.chat.ChatMessageList
import xyz.larkzhh.lime.ui.detail.components.ImagePreviewOverlay
import xyz.larkzhh.lime.ui.theme.LimeGray
import xyz.larkzhh.lime.ui.theme.LimeLightGray
import xyz.larkzhh.lime.ui.theme.LimePrimary
import xyz.larkzhh.lime.util.copyToClipboard
import xyz.larkzhh.lime.util.formatRelativeTime
import xyz.larkzhh.lime.util.showToast
import kotlin.math.abs
import kotlin.math.roundToInt

/// 抽屉占屏宽比例
private const val DRAWER_WIDTH_FRACTION = 0.82f

@Composable
fun AiChatScreen(
    viewModel: AiChatViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val conversations = viewModel.conversations.collectAsLazyPagingItems()

    // 图片预览
    var previewImages by remember { mutableStateOf<List<String>>(emptyList()) }
    var previewIndex by remember { mutableIntStateOf(0) }
    // 抽屉长按操作菜单
    var menuTarget by remember { mutableStateOf<ChatConversation?>(null) }
    var deleteTarget by remember { mutableStateOf<ChatConversation?>(null) }
    // 输入栏底部栏
    var showAddSheet by remember { mutableStateOf(false) }

    // 选图
    val pickImagesLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(maxItems = 4)
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.addImages(uris.map { it.toString() })
        }
    }

    // 错误提示
    LaunchedEffect(state.error) {
        state.error?.let {
            it.showToast(context)
            viewModel.consumeError()
        }
    }

    val messagesById = remember(state.messages) { state.messages.associateBy { it.localId } }
    val bubbleList = remember(state.messages) {
        state.messages
            .filterNot {
                it.role == ChatRole.ASSISTANT &&
                        it.content.isBlank() && it.images.isEmpty() && it.localImageUris.isEmpty()
            }
            .map { it.toBubbleData() }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val drawerWidthPx = constraints.maxWidth * DRAWER_WIDTH_FRACTION
        val drawerWidthDp = with(density) { drawerWidthPx.toDp() }
        val progress = remember { Animatable(0f) }
        var drawerOpen by remember { mutableStateOf(false) }
        val scope = rememberCoroutineScope()

        // 打开抽屉
        fun openDrawer() {
            drawerOpen = true
            scope.launch { progress.animateTo(1f, tween(240)) }
        }

        // 关闭抽屉
        fun closeDrawer() {
            drawerOpen = false
            scope.launch { progress.animateTo(0f, tween(240)) }
        }

        // 收起抽屉
        BackHandler(enabled = drawerOpen) { closeDrawer() }

        // 打开抽屉刷新会话列表
        LaunchedEffect(drawerOpen) {
            if (drawerOpen) conversations.refresh()
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                // 监听左缘手势
                .pointerInput(drawerWidthPx, drawerOpen) {
                    if (!drawerOpen) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            val edgePx = 40.dp.toPx()
                            if (down.position.x > edgePx) return@awaitEachGesture
                            val touchSlop = viewConfiguration.touchSlop
                            var drag = 0f
                            var intercepted = false
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull() ?: break
                                if (!change.pressed) break
                                val delta = change.position.x - change.previousPosition.x
                                drag += delta
                                if (!intercepted && abs(drag) > touchSlop) {
                                    intercepted = true
                                    drawerOpen = true
                                }
                                if (intercepted) {
                                    change.consume()
                                    scope.launch {
                                        progress.snapTo((drag / drawerWidthPx).coerceIn(0f, 1f))
                                    }
                                }
                            }
                            if (intercepted) {
                                if (progress.value > 0.5f) openDrawer() else closeDrawer()
                            }
                        }
                    }
                },
        ) {
            // 抽屉层
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(drawerWidthDp)
                    .offset {
                        IntOffset(
                            (-drawerWidthPx * (1f - progress.value)).roundToInt(),
                            0,
                        )
                    }
                    .background(MaterialTheme.colorScheme.surface),
            ) {
                DrawerContent(
                    conversations = conversations,
                    currentId = state.serverConversationId,
                    onNewConversation = {
                        viewModel.startNewConversation()
                        closeDrawer()
                    },
                    onOpenConversation = { conversation ->
                        viewModel.openConversation(conversation.id)
                        closeDrawer()
                    },
                    onLongPressConversation = { menuTarget = it },
                )
            }

            // 聊天层
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .offset { IntOffset((drawerWidthPx * progress.value).roundToInt(), 0) },
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .imePadding(),
                ) {
                    // 顶部栏
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(onClick = { if (drawerOpen) closeDrawer() else openDrawer() }) {
                            Icon(
                                Icons.Outlined.Menu,
                                contentDescription = "打开历史会话",
                                tint = MaterialTheme.colorScheme.onBackground,
                            )
                        }
                        Text(
                            text = state.title.ifBlank { "新对话" },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        // 模型选择
                        Box {
                            TextButton(onClick = viewModel::toggleModelPicker) {
                                Text(
                                    text = state.models.firstOrNull { it.name == state.selectedModel }?.displayName
                                        ?: "默认模型",
                                    color = LimeGray,
                                    fontSize = 13.sp,
                                )
                            }
                            DropdownMenu(
                                expanded = state.showModelPicker,
                                onDismissRequest = { viewModel.toggleModelPicker() },
                            ) {
                                DropdownMenuItem(
                                    text = { Text("默认模型") },
                                    onClick = { viewModel.selectModel(null) },
                                )
                                state.models.forEach { model ->
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(
                                                    text = model.displayName,
                                                    fontWeight = if (model.name == state.selectedModel) {
                                                        FontWeight.SemiBold
                                                    } else {
                                                        FontWeight.Normal
                                                    },
                                                )
                                                if (!model.description.isNullOrBlank()) {
                                                    Text(
                                                        text = model.description,
                                                        fontSize = 11.sp,
                                                        color = LimeGray,
                                                    )
                                                }
                                            }
                                        },
                                        onClick = { viewModel.selectModel(model) },
                                    )
                                }
                            }
                        }
                        // 清空对话
                        IconButton(
                            onClick = viewModel::requestClearConversation,
                            enabled = state.serverConversationId != null,
                        ) {
                            Icon(
                                Icons.Outlined.DeleteSweep,
                                contentDescription = "清空对话",
                                tint = if (state.serverConversationId != null) LimeGray
                                else LimeGray.copy(alpha = 0.3f),
                                modifier = Modifier.size(21.dp),
                            )
                        }
                    }

                    // 离线横幅
                    if (state.isOffline) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(LimeLightGray)
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.Outlined.WifiOff,
                                contentDescription = null,
                                tint = LimeGray,
                                modifier = Modifier.size(14.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("当前无网络，显示缓存消息", color = LimeGray, fontSize = 12.sp)
                        }
                    }

                    // 消息列表
                    ChatMessageList(
                        messages = bubbleList,
                        typing = false,
                        modifier = Modifier.weight(1f),
                        onRetry = { bubble ->
                            messagesById[bubble.id]?.let { viewModel.retryMessage(it) }
                        },
                        onCopy = { bubble ->
                            bubble.content.copyToClipboard(context)
                            "已复制".showToast(context)
                        },
                        onImageClick = { index, images ->
                            previewImages = images
                            previewIndex = index
                        },
                    )

                    // 输入栏
                    ChatInputBar(
                        text = state.inputText,
                        onTextChange = viewModel::onInputChange,
                        images = state.pendingImages.map {
                            ChatInputImage(
                                uri = it.localUri,
                                state = when (it.state) {
                                    PendingImageState.PENDING -> ChatInputImageState.PENDING
                                    PendingImageState.UPLOADING -> ChatInputImageState.UPLOADING
                                    PendingImageState.FAILED -> ChatInputImageState.FAILED
                                },
                            )
                        },
                        sending = state.streaming || state.busy,
                        canSend = (state.inputText.isNotBlank() || state.pendingImages.isNotEmpty()) && !state.isOffline,
                        onAddClick = { showAddSheet = true },
                        onRemoveImage = viewModel::removeImage,
                        onRetryImage = viewModel::retryImage,
                        onSend = viewModel::send,
                        onStop = viewModel::stop,
                    )
                }

                // 抽屉打开时遮罩
                if (progress.value > 0.01f) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.25f * progress.value))
                            .pointerInput(Unit) {
                                detectHorizontalDragGestures(
                                    onHorizontalDrag = { change, delta ->
                                        change.consume()
                                        scope.launch {
                                            progress.snapTo(
                                                (progress.value + delta / drawerWidthPx).coerceIn(
                                                    0f,
                                                    1f
                                                )
                                            )
                                        }
                                    },
                                    onDragEnd = {
                                        if (progress.value < 0.5f) closeDrawer() else openDrawer()
                                    },
                                )
                            }
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            ) { closeDrawer() },
                    )
                }
            }
        }

        // 覆盖层
        menuTarget?.let { target ->
            BottomActionSheet(
                visible = true,
                onDismiss = { menuTarget = null },
                actions = listOf(
                    SheetAction(
                        label = "清空对话",
                        onClick = {
                            viewModel.clearMessages(target) { ok ->
                                if (ok) "已清空".showToast(context)
                            }
                            closeDrawer()
                        },
                    ),
                    SheetAction(
                        label = "删除会话",
                        textColor = MaterialTheme.colorScheme.error,
                        onClick = { deleteTarget = target },
                    ),
                    SheetAction(
                        label = "取消",
                        textColor = LimeGray,
                        onClick = {},
                    ),
                ),
            )
        }

        deleteTarget?.let { target ->
            LimeAlertDialog(
                title = "删除会话",
                text = "「${target.title}」将被删除，且不可恢复。",
                firstButtonText = "取消",
                secondButtonText = "删除",
                secondButtonColor = MaterialTheme.colorScheme.error,
                onFirstButtonClick = { deleteTarget = null },
                onSecondButtonClick = {
                    viewModel.deleteConversation(target) { ok ->
                        if (ok) "会话已删除".showToast(context)
                    }
                    deleteTarget = null
                    closeDrawer()
                },
                onDismissRequest = { deleteTarget = null },
            )
        }

        if (state.showClearDialog) {
            LimeAlertDialog(
                title = "清空对话",
                text = "将清空该会话的全部消息，且不可恢复。",
                firstButtonText = "取消",
                secondButtonText = "清空",
                secondButtonColor = MaterialTheme.colorScheme.error,
                onFirstButtonClick = viewModel::dismissClearDialog,
                onSecondButtonClick = viewModel::clearConversation,
                onDismissRequest = viewModel::dismissClearDialog,
            )
        }

        // 输入栏底部栏
        if (showAddSheet) {
            ChatAddSheet(
                onDismiss = { showAddSheet = false },
                onCamera = {
                    showAddSheet = false
                    "拍照功能开发中".showToast(context)
                },
                onAlbum = {
                    showAddSheet = false
                    pickImagesLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                onNote = {
                    showAddSheet = false
                    "笔迹功能开发中".showToast(context)
                },
            )
        }

        if (previewImages.isNotEmpty()) {
            ImagePreviewOverlay(
                images = previewImages,
                initialIndex = previewIndex,
                onDismiss = { previewImages = emptyList() },
            )
        }
    }
}

// 抽屉内容
@Composable
private fun DrawerContent(
    conversations: LazyPagingItems<ChatConversation>,
    currentId: String?,
    onNewConversation: () -> Unit,
    onOpenConversation: (ChatConversation) -> Unit,
    onLongPressConversation: (ChatConversation) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        // 标题、新建对话
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(start = 18.dp, end = 10.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "对话记录",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onNewConversation) {
                Icon(
                    Icons.Outlined.Add,
                    contentDescription = null,
                    tint = LimePrimary,
                    modifier = Modifier.size(17.dp),
                )
                Spacer(Modifier.width(2.dp))
                Text("新对话", color = LimePrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))

        when {
            // 空态
            conversations.itemCount == 0 &&
                    conversations.loadState.refresh !is LoadState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("暂无历史对话", color = LimeGray, fontSize = 13.sp)
                }
            }

            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
            ) {
                items(
                    count = conversations.itemCount,
                    key = { index -> conversations[index]?.id ?: index },
                ) { index ->
                    val conversation = conversations[index] ?: return@items
                    DrawerConversationRow(
                        conversation = conversation,
                        selected = conversation.id == currentId,
                        onClick = { onOpenConversation(conversation) },
                        onLongClick = { onLongPressConversation(conversation) },
                    )
                }
                when (conversations.loadState.append) {
                    is LoadState.Loading -> item(key = "loading_more") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = LimePrimary,
                            )
                        }
                    }

                    is LoadState.Error -> item(key = "load_more_error") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            TextButton(onClick = { conversations.retry() }) {
                                Text("加载失败，点击重试", color = LimeGray, fontSize = 12.sp)
                            }
                        }
                    }

                    else -> Unit
                }
            }
        }
    }
}

@Composable
private fun DrawerConversationRow(
    conversation: ChatConversation,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (selected) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
                else Color.Transparent
            )
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = conversation.title.ifBlank { "新对话" },
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = formatRelativeTime(conversation.updateTime),
                color = LimeGray,
                fontSize = 11.sp,
            )
        }
    }
}

/// 域模型转通用气泡模型
private fun ChatMessage.toBubbleData(): ChatBubbleData = ChatBubbleData(
    id = localId,
    isSelf = role == ChatRole.USER,
    content = content,
    images = images.ifEmpty { localImageUris },
    timestamp = createTime,
    status = when (status) {
        ChatMessageStatus.SENDING -> ChatBubbleStatus.SENDING
        ChatMessageStatus.STREAMING -> ChatBubbleStatus.STREAMING
        ChatMessageStatus.DONE -> ChatBubbleStatus.DONE
        ChatMessageStatus.FAILED -> ChatBubbleStatus.FAILED
        ChatMessageStatus.STOPPED -> ChatBubbleStatus.STOPPED
    },
    renderMarkdown = role == ChatRole.ASSISTANT,
)
