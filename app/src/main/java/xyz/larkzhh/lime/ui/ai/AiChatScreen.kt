package xyz.larkzhh.lime.ui.ai

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.draw.shadow
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
import xyz.larkzhh.lime.domain.model.ChatNote
import xyz.larkzhh.lime.domain.model.ChatRole
import xyz.larkzhh.lime.ui.components.LimeAlertDialog
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
import xyz.larkzhh.lime.util.stripMarkdown
import xyz.larkzhh.lime.util.TtsManager
import kotlin.math.abs
import kotlin.math.roundToInt

/// 抽屉占屏宽比例
private const val DRAWER_WIDTH_FRACTION = 0.82f

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiChatScreen(
    viewModel: AiChatViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val conversations = viewModel.conversations.collectAsLazyPagingItems()

    DisposableEffect(Unit) {
        onDispose { TtsManager.shutdown() }
    }

    // 图片预览
    var previewImages by remember { mutableStateOf<List<String>>(emptyList()) }
    var previewIndex by remember { mutableIntStateOf(0) }
    // 待删除的会话
    var deleteTarget by remember { mutableStateOf<ChatConversation?>(null) }
    // 输入栏底部栏
    var showAddSheet by remember { mutableStateOf(false) }
    // 笔记选择器
    var showNotePicker by remember { mutableStateOf(false) }
    // 纯文本内容
    var selectTextContent by remember { mutableStateOf<String?>(null) }
    // 待删除的消息
    var pendingDeleteMessage by remember { mutableStateOf<ChatMessage?>(null) }

    // 选图
    val pickImagesLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(maxItems = 4)
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.addImages(uris.map { it.toString() })
        }
    }

    // 拍照
    var cameraUri by remember { mutableStateOf<Uri?>(null) }
    val takePictureLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            cameraUri?.let { viewModel.addImages(listOf(it.toString())) }
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
        // 关闭笔记选择页
        BackHandler(enabled = showNotePicker) { showNotePicker = false }

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
                    onClearConversation = { conversation ->
                        viewModel.clearMessages(conversation) { ok ->
                            if (ok) "已清空".showToast(context)
                        }
                        closeDrawer()
                    },
                    onDeleteConversation = { conversation ->
                        deleteTarget = conversation
                    },
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
                        .background(LimeLightGray)
                        .imePadding(),
                ) {
                    // 顶部栏
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // 菜单按钮
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .shadow(2.dp, CircleShape)
                                .clip(CircleShape)
                                .background(Color.White)
                                .clickable { if (drawerOpen) closeDrawer() else openDrawer() },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Outlined.Menu,
                                contentDescription = "打开历史会话",
                                tint = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        // 标题、模型选择
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = state.title.ifBlank { "新对话" },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Box {
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .clickable { viewModel.toggleModelPicker() }
                                        .padding(horizontal = 4.dp, vertical = 0.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = state.models.firstOrNull { it.name == state.selectedModel }?.displayName
                                            ?: "默认模型",
                                        color = LimeGray,
                                        fontSize = 12.sp,
                                    )
                                    Icon(
                                        Icons.Filled.ArrowDropDown,
                                        contentDescription = null,
                                        tint = LimeGray,
                                        modifier = Modifier.size(14.dp),
                                    )
                                }
                                DropdownMenu(
                                    expanded = state.showModelPicker,
                                    onDismissRequest = { viewModel.toggleModelPicker() },
                                    containerColor = MaterialTheme.colorScheme.surface,
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
                        }
                        // 新建会话、清空
                        Row(
                            modifier = Modifier
                                .shadow(2.dp, RoundedCornerShape(20.dp))
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color.White),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            IconButton(onClick = { viewModel.startNewConversation() }) {
                                Icon(
                                    Icons.Outlined.Add,
                                    contentDescription = "新建会话",
                                    tint = MaterialTheme.colorScheme.onBackground,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                            IconButton(
                                onClick = viewModel::requestClearConversation,
                                enabled = state.serverConversationId != null,
                            ) {
                                Icon(
                                    Icons.Outlined.DeleteSweep,
                                    contentDescription = "清空对话",
                                    tint = if (state.serverConversationId != null) {
                                        MaterialTheme.colorScheme.onBackground
                                    } else {
                                        LimeGray.copy(alpha = 0.4f)
                                    },
                                    modifier = Modifier.size(20.dp),
                                )
                            }
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
                        onRegenerate = { bubble ->
                            messagesById[bubble.id]?.let { viewModel.regenerate(it) }
                        },
                        onSpeak = { bubble ->
                            TtsManager.speak(context, bubble.id, bubble.content)
                        },
                        onDelete = { bubble ->
                            messagesById[bubble.id]?.let { pendingDeleteMessage = it }
                        },
                        onSelectText = { bubble ->
                            selectTextContent = bubble.content
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
                        canSend = (state.inputText.isNotBlank() || state.pendingImages.isNotEmpty() || state.selectedNote != null) && !state.isOffline,
                        note = state.selectedNote,
                        onRemoveNote = viewModel::removeNote,
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

        // 删除单条消息确认
        pendingDeleteMessage?.let { msg ->
            LimeAlertDialog(
                title = "删除消息",
                text = "将删除这条消息及其关联的提问/回复，且不可恢复。",
                firstButtonText = "取消",
                secondButtonText = "删除",
                secondButtonColor = MaterialTheme.colorScheme.error,
                onFirstButtonClick = { pendingDeleteMessage = null },
                onSecondButtonClick = {
                    viewModel.deleteMessagePair(msg)
                    pendingDeleteMessage = null
                },
                onDismissRequest = { pendingDeleteMessage = null },
            )
        }

        // 输入栏底部栏
        if (showAddSheet) {
            ChatAddSheet(
                onDismiss = { showAddSheet = false },
                onCamera = {
                    showAddSheet = false
                    val uri = createCameraImageUri(context)
                    if (uri != null) {
                        cameraUri = uri
                        takePictureLauncher.launch(uri)
                    } else {
                        "无法创建拍照文件".showToast(context)
                    }
                },
                onAlbum = {
                    showAddSheet = false
                    pickImagesLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                onNote = {
                    showAddSheet = false
                    showNotePicker = true
                },
            )
        }

        // 笔记选择页（
        AnimatedVisibility(
            visible = showNotePicker,
            enter = slideInHorizontally(animationSpec = tween(280), initialOffsetX = { it }) + fadeIn(),
            exit = slideOutHorizontally(animationSpec = tween(280), targetOffsetX = { it }) + fadeOut(),
        ) {
            NotePickerPage(
                onDismiss = { showNotePicker = false },
                onConfirm = { item ->
                    showNotePicker = false
                    viewModel.selectNote(ChatNote(id = item.id, title = item.title, cover = item.coverImage))
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

        // 选取文字底部栏
        selectTextContent?.let { content ->
            val pureText = content.stripMarkdown()
            ModalBottomSheet(
                onDismissRequest = { selectTextContent = null },
                containerColor = MaterialTheme.colorScheme.surface,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 24.dp),
                ) {
                    Text(
                        text = "选择文本",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(12.dp))
                    SelectionContainer {
                        Text(
                            text = pureText,
                            style = MaterialTheme.typography.bodyLarge,
                            lineHeight = 24.sp,
                        )
                    }
                }
            }
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
    onClearConversation: (ChatConversation) -> Unit,
    onDeleteConversation: (ChatConversation) -> Unit,
    modifier: Modifier = Modifier,
) {
    var menuId by remember { mutableStateOf<String?>(null) }
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
                    Box {
                        DrawerConversationRow(
                            conversation = conversation,
                            selected = conversation.id == currentId,
                            onClick = { onOpenConversation(conversation) },
                            onLongClick = { menuId = conversation.id },
                        )
                        // 长按菜单
                        DropdownMenu(
                            expanded = menuId == conversation.id,
                            onDismissRequest = { menuId = null },
                            containerColor = MaterialTheme.colorScheme.surface,
                        ) {
                            DropdownMenuItem(
                                text = { Text("清空对话") },
                                onClick = {
                                    menuId = null
                                    onClearConversation(conversation)
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("删除会话", color = MaterialTheme.colorScheme.error) },
                                onClick = {
                                    menuId = null
                                    onDeleteConversation(conversation)
                                },
                            )
                        }
                    }
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
    note = note,
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

/// 在创建空白图片
private fun createCameraImageUri(context: Context): Uri? {
    val values = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, "lime_${System.currentTimeMillis()}.jpg")
        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
    }
    return runCatching {
        context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
    }.getOrNull()
}
