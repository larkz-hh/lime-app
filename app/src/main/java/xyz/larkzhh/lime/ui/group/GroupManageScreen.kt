package xyz.larkzhh.lime.ui.group

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import coil3.compose.AsyncImage
import xyz.larkzhh.lime.R
import xyz.larkzhh.lime.data.network.model.UserData
import xyz.larkzhh.lime.ui.components.LimeAlertDialog
import xyz.larkzhh.lime.ui.theme.LimeGray
import xyz.larkzhh.lime.util.showToast

private val CheckBorderGray = Color(0xFFCCCCCC)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupManageScreen(
    groupId: String,
    navController: NavHostController,
    viewModel: GroupManageViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val inviteSentText = stringResource(R.string.group_invite_sent)
    val saveSuccessText = stringResource(R.string.group_save_success)
    var name by remember { mutableStateOf("") }
    var introduction by remember { mutableStateOf("") }
    var nameInitialized by remember { mutableStateOf(false) }
    var showConfirm by remember { mutableStateOf(false) }// 解散/退出确认
    var showFriendPicker by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf<Set<Long>>(emptySet()) }

    LaunchedEffect(groupId) {
        viewModel.load(groupId)
    }
    // 保存成功提示
    LaunchedEffect(Unit) {
        viewModel.saved.collect { saveSuccessText.showToast(context) }
    }
    LaunchedEffect(state.group) {
        val g = state.group
        if (g != null && !nameInitialized) {
            name = g.name
            introduction = g.introduction ?: ""
            nameInitialized = true
        }
    }
    LaunchedEffect(state.done) {
        if (state.done) navController.popBackStack()
    }

    val avatarPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) viewModel.uploadAvatar(uri)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(stringResource(R.string.group_manage_title), fontWeight = FontWeight.Bold, fontSize = 17.sp, color = MaterialTheme.colorScheme.onSurface)
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back), tint = MaterialTheme.colorScheme.onSurface)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = Color.Unspecified,
                    navigationIconContentColor = Color.Unspecified,
                    titleContentColor = Color.Unspecified,
                    actionIconContentColor = Color.Unspecified
                ),
            )
        },
    ) { padding ->
        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(20.dp))
            // 群头像
            AsyncImage(
                model = state.group?.faceUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(84.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface)
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                        enabled = state.isOwner,
                        onClick = { avatarPicker.launch("image/*") },
                    ),
            )
            if (state.isOwner) {
                Spacer(Modifier.height(6.dp))
                Text(stringResource(R.string.group_avatar_change_hint), color = LimeGray, fontSize = 12.sp)
            }
            Spacer(Modifier.height(16.dp))

            GroupFieldCard(
                label = stringResource(R.string.group_name),
                required = true,
                value = name,
                onValueChange = { name = it },
                placeholder = stringResource(R.string.group_name_hint),
                maxLength = 24,
                singleLine = true,
                enabled = state.isOwner,
            )
            Spacer(Modifier.height(16.dp))
            GroupFieldCard(
                label = stringResource(R.string.group_manage_intro),
                required = true,
                value = introduction,
                onValueChange = { introduction = it },
                placeholder = stringResource(R.string.group_manage_intro_hint),
                maxLength = 100,
                singleLine = false,
                minLines = 3,
                enabled = state.isOwner,
            )
            state.error?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
            }
            if (state.isOwner) {
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = { viewModel.saveInfo(name, introduction) },
                    enabled = !state.isSaving,
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                ) {
                    if (state.isSaving) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(18.dp),
                        )
                    } else {
                        Text(stringResource(R.string.save))
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            // 邀请好友
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                        onClick = { showFriendPicker = true },
                    )
                    .padding(horizontal = 16.dp, vertical = 15.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.PersonAdd, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.width(12.dp))
                Text(stringResource(R.string.group_invite_friends), fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.weight(1f))
                Text(stringResource(R.string.group_member_count, state.group?.memberCount ?: 0), fontSize = 12.sp, color = LimeGray)
            }

            Spacer(Modifier.height(30.dp))
            // 解散或退出
            TextButton(
                onClick = { showConfirm = true },
                enabled = !state.isSaving,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    if (state.isOwner) stringResource(R.string.group_dismiss_group) else stringResource(R.string.group_quit_group),
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    // 解散或退出确认
    if (showConfirm) {
        LimeAlertDialog(
            title = if (state.isOwner) stringResource(R.string.group_dismiss_group) else stringResource(R.string.group_quit_group),
            text = if (state.isOwner) stringResource(R.string.group_dismiss_message) else stringResource(R.string.group_quit_message),
            firstButtonText = stringResource(R.string.cancel),
            secondButtonText = if (state.isOwner) stringResource(R.string.group_dismiss) else stringResource(R.string.group_quit),
            secondButtonColor = MaterialTheme.colorScheme.error,
            onDismissRequest = { showConfirm = false },
            onFirstButtonClick = { showConfirm = false },
            onSecondButtonClick = {
                showConfirm = false
                if (state.isOwner) viewModel.dismissGroup() else viewModel.quitGroup()
            },
        )
    }

    // 好友选择
    if (showFriendPicker) {
        AlertDialog(
            onDismissRequest = { showFriendPicker = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text(stringResource(R.string.group_pick_friends_title), fontWeight = FontWeight.Bold, fontSize = 17.sp) },
            text = {
                if (state.mutualFriends.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(stringResource(R.string.group_no_mutual_friends), color = LimeGray, fontSize = 14.sp)
                    }
                } else {
                    LazyColumn(modifier = Modifier.height(360.dp)) {
                        items(state.mutualFriends, key = { it.id }) { friend ->
                            FriendRow(
                                friend = friend,
                                checked = friend.id in selectedIds,
                                onToggle = {
                                    selectedIds = if (friend.id in selectedIds) selectedIds - friend.id else selectedIds + friend.id
                                },
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val selected = state.mutualFriends.filter { it.id in selectedIds }
                        if (selected.isNotEmpty()) {
                            viewModel.invite(selected)
                            inviteSentText.showToast(context)
                        }
                        selectedIds = emptySet()
                        showFriendPicker = false
                    },
                ) { Text(stringResource(R.string.group_invite)) }
            },
            dismissButton = {
                TextButton(onClick = { showFriendPicker = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}

@Composable
private fun FriendRow(friend: UserData, checked: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onToggle,
            )
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (friend.avatar.isNullOrBlank()) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = friend.nickname.take(1),
                    fontSize = 16.sp,
                    color = LimeGray,
                    fontWeight = FontWeight.Bold,
                )
            }
        } else {
            AsyncImage(
                model = friend.avatar,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(friend.nickname, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(friend.handle, fontSize = 12.sp, color = LimeGray, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.width(12.dp))
        CircleCheck(checked = checked, onToggle = onToggle)
    }
}

/// 圆形勾选框
@Composable
private fun CircleCheck(checked: Boolean, onToggle: () -> Unit) {
    Box(
        modifier = Modifier
            .size(22.dp)
            .clip(CircleShape)
            .background(if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface)
            .border(
                width = if (checked) 0.dp else 1.5.dp,
                color = if (checked) Color.Transparent else CheckBorderGray,
                shape = CircleShape,
            )
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onToggle,
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (checked) {
            Icon(
                Icons.Filled.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}
