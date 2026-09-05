package xyz.larkzhh.lime.ui.im

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import kotlin.math.roundToInt
import coil3.compose.AsyncImage
import xyz.larkzhh.lime.domain.model.ImMessage
import xyz.larkzhh.lime.navigation.navigateToUserProfile
import xyz.larkzhh.lime.ui.components.LimeAlertDialog
import xyz.larkzhh.lime.ui.components.chat.ChatInputBar
import xyz.larkzhh.lime.ui.detail.components.EmojiPanel
import xyz.larkzhh.lime.ui.im.viewmodel.ImChatViewModel
import xyz.larkzhh.lime.ui.theme.LimeGray
import xyz.larkzhh.lime.ui.theme.LimeLightGray
import xyz.larkzhh.lime.util.copyToClipboard
import xyz.larkzhh.lime.util.copyUriToCache
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    conversationId: String,
    onBack: () -> Unit,
    navController: NavHostController,
    viewModel: ImChatViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var inputText by remember { mutableStateOf("") }
    var showEmojiPanel by remember { mutableStateOf(false) }
    var pendingKeyboard by remember { mutableStateOf(false) }
    var showClearConfirm by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val density = LocalDensity.current
    // 记忆键盘最大高度
    var savedImeHeight by remember { mutableIntStateOf(0) }
    val imeHeightPx = WindowInsets.ime.getBottom(density)
    LaunchedEffect(imeHeightPx) {
        val dp = with(density) { imeHeightPx.toDp().value.toInt() }
        if (dp > savedImeHeight) savedImeHeight = dp
        if (pendingKeyboard && savedImeHeight > 0 && dp >= savedImeHeight) {
            showEmojiPanel = false
            pendingKeyboard = false
        }
    }

    LaunchedEffect(conversationId) {
        viewModel.load(conversationId)
    }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { context.copyUriToCache(it, "im")?.let(viewModel::sendImage) }
    }

    if (showClearConfirm) {
        LimeAlertDialog(
            title = "清空聊天记录",
            text = "确定要清空与对方的历史消息吗？此操作不可恢复。",
            firstButtonText = "取消",
            secondButtonText = "清空",
            secondButtonColor = Color(0xFFFE2C55),
            onFirstButtonClick = { showClearConfirm = false },
            onSecondButtonClick = {
                showClearConfirm = false
                viewModel.clearHistory()
            },
            onDismissRequest = { showClearConfirm = false },
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(state.peerNickname ?: "私信") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { showClearConfirm = true }) {
                        Icon(
                            Icons.Outlined.Delete,
                            contentDescription = "清空聊天记录",
                            tint = LimeGray,
                        )
                    }
                },
            )
        },
        bottomBar = {
            val emojiHeight = savedImeHeight.dp.coerceAtLeast(260.dp)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .let { if (!showEmojiPanel && !pendingKeyboard) it.imePadding() else it },
            ) {
                ChatInputBar(
                    text = inputText,
                    onTextChange = { inputText = it },
                    focusRequester = focusRequester,
                    placeholder = "发消息…",
                    canSend = inputText.isNotBlank(),
                    onAddClick = { imagePicker.launch("image/*") },
                    onSend = {
                        if (inputText.isNotBlank()) {
                            viewModel.sendText(inputText)
                            inputText = ""
                        }
                    },
                    showEmojiToggle = true,
                    emojiActive = showEmojiPanel,
                    onEmojiClick = {
                        if (showEmojiPanel) {
                            pendingKeyboard = true
                            focusRequester.requestFocus()
                            keyboardController?.show()
                        } else {
                            focusManager.clearFocus()
                            showEmojiPanel = true
                        }
                    },
                )
                when {
                    showEmojiPanel -> EmojiPanel(
                        onEmojiClick = { emoji -> inputText += emoji },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(emojiHeight),
                    )

                    pendingKeyboard -> Spacer(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(emojiHeight),
                    )
                }
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            reverseLayout = true,
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(state.messages, key = { it.id }) { msg ->
                MessageBubble(
                    message = msg,
                    avatar = if (msg.isSelf) state.selfAvatar else state.peerAvatar,
                    onAvatarClick = {
                        val target = if (msg.isSelf) state.selfUserId else state.peerUserId
                        target?.let { navController.navigateToUserProfile(it, state.selfUserId) }
                    },
                    onCopy = { msg.text.orEmpty().copyToClipboard(context) },
                    onRevoke = { viewModel.revokeMessage(msg) },
                    onDelete = { viewModel.deleteMessage(msg) },
                )
            }
        }
    }
}

/// 头像
@Composable
private fun AvatarView(
    avatar: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AsyncImage(
        model = avatar,
        contentDescription = "头像",
        contentScale = ContentScale.Crop,
        modifier = modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(LimeLightGray)
            .clickable(onClick = onClick),
    )
}


@Composable
private fun MessageBubble(
    message: ImMessage,
    avatar: String?,
    onAvatarClick: () -> Unit,
    onCopy: () -> Unit,
    onRevoke: () -> Unit,
    onDelete: () -> Unit,
) {
    // 撤回消息提示
    if (message.isRevoked) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = if (message.isSelf) "你撤回了一条消息" else "对方撤回了一条消息",
                color = LimeGray,
                fontSize = 12.sp,
            )
        }
        return
    }

    var showMenu by remember { mutableStateOf(false) }
    var longPressOffset by remember { mutableStateOf(Offset.Zero) }
    val canRevoke = message.isSelf &&
        (System.currentTimeMillis() / 1000 - message.timestamp) <= 120
    val bubbleColor = if (message.isSelf) Color(0xFF3D5AFE) else Color(0xFFF1F1F1)
    val contentColor = if (message.isSelf) Color.White else Color(0xFF111111)

    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = {},
                        onLongPress = { offset ->
                            longPressOffset = offset
                            showMenu = true
                        },
                    )
                },
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = if (message.isSelf) Arrangement.End else Arrangement.Start,
        ) {
            if (!message.isSelf) {
                AvatarView(avatar, onAvatarClick, Modifier.padding(end = 6.dp))
            }
            when {
                message.isImage -> AsyncImage(
                    model = message.imagePath?.let { File(it) } ?: message.imageUrl,
                    contentDescription = "图片消息",
                    modifier = Modifier
                        .size(width = 200.dp, height = 200.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop,
                )
                else -> Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(bubbleColor)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    Text(message.text.orEmpty(), color = contentColor)
                }
            }
            if (message.isSelf) {
                AvatarView(avatar, onAvatarClick, Modifier.padding(start = 6.dp))
            }
        }

        // 长按菜单
        Box(
            modifier = Modifier.offset {
                IntOffset(longPressOffset.x.roundToInt(), longPressOffset.y.roundToInt())
            },
        ) {
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false },
                containerColor = Color.White,
                modifier = Modifier,
            ) {
            if (!message.text.isNullOrBlank()) {
                DropdownMenuItem(
                    text = { Text("复制") },
                    leadingIcon = { Icon(Icons.Filled.ContentCopy, contentDescription = null) },
                    onClick = { showMenu = false; onCopy() },
                )
            }
            if (canRevoke) {
                DropdownMenuItem(
                    text = { Text("撤回") },
                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = null) },
                    onClick = { showMenu = false; onRevoke() },
                )
            }
            DropdownMenuItem(
                text = { Text("删除") },
                leadingIcon = {
                    Icon(Icons.Outlined.Delete, contentDescription = null)
                },
                onClick = { showMenu = false; onDelete() },
            )
            }
        }
    }
}

