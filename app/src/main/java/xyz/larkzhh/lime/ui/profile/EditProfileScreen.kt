package xyz.larkzhh.lime.ui.profile

import android.app.Activity
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import coil3.compose.AsyncImage
import com.yalantis.ucrop.UCrop
import xyz.larkzhh.lime.ui.profile.viewmodel.EditProfileUiState
import xyz.larkzhh.lime.ui.profile.viewmodel.EditProfileViewModel
import xyz.larkzhh.lime.ui.theme.LimeDark
import xyz.larkzhh.lime.ui.theme.LimeGray
import xyz.larkzhh.lime.ui.theme.LimeLightGray
import xyz.larkzhh.lime.ui.theme.LimePrimary
import java.io.File
import xyz.larkzhh.lime.ui.profile.components.WheelDatePicker

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(navController: NavHostController) {
    val viewModel: EditProfileViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val avatarLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { viewModel.uploadAvatar(it) }
    }

    /// 接收裁剪结果后上传
    val bgCropLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            UCrop.getOutput(result.data!!)?.let { viewModel.uploadBackground(it) }
        }// 提取出裁剪后保存在本地沙盒的 Uri
    }

    /// 选图后跳转裁剪页
    val bgLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            val dest = Uri.fromFile(File(context.cacheDir, "bg_crop_tmp.jpg"))
            val intent = UCrop.of(uri, dest)
                .withOptions(UCrop.Options().apply {
                    setToolbarTitle("截取背景图")
                    setCompressionQuality(90)
                    setFreeStyleCropEnabled(true)// 允许自由比例裁剪
                    setToolbarColor(0xFFFFFFFF.toInt())
                    setStatusBarColor(0xFF1A1A1A.toInt())
                    setActiveControlsWidgetColor(0xFF4A9B6F.toInt())
                })
                .getIntent(context)
            bgCropLauncher.launch(intent)
        }
    }

    var showNicknameDialog by remember { mutableStateOf(false) }
    var showBioDialog by remember { mutableStateOf(false) }
    var showGenderDialog by remember { mutableStateOf(false) }
    var showRegionDialog by remember { mutableStateOf(false) }
    var showBirthdayPicker by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState) {
        val ready = uiState as? EditProfileUiState.Ready
        if (ready?.done == true) navController.popBackStack()// 保存成功后自动返回
        // 上传错误时弹出提示
        ready?.uploadError?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearUploadError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("编辑资料", fontWeight = FontWeight.Medium) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    val ready = uiState as? EditProfileUiState.Ready
                    TextButton(
                        onClick = { viewModel.saveProfile() },
                        enabled = ready != null && !ready.isSaving,
                    ) {
                        Text("保存", color = LimePrimary, fontWeight = FontWeight.Medium)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White),
            )
        },
        containerColor = LimeLightGray,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { paddingValues ->
        when (val state = uiState) {
            is EditProfileUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = LimePrimary)
                }
            }

            is EditProfileUiState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(state.message, color = LimeGray)
                }
            }

            is EditProfileUiState.Ready -> {
                val form = state.form

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .verticalScroll(rememberScrollState()),
                ) {
                    Spacer(Modifier.height(24.dp))

                    // 头像
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Box(
                            modifier = Modifier
                                .size(88.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFD4EAE0))
                                .clickable { avatarLauncher.launch("image/*") },
                            contentAlignment = Alignment.Center,
                        ) {
                            if (form.avatarUrl != null) {
                                AsyncImage(
                                    model = form.avatarUrl,
                                    contentDescription = "头像",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop,
                                )
                            } else {
                                Icon(
                                    Icons.Default.CameraAlt,
                                    contentDescription = null,
                                    tint = LimeGray,
                                    modifier = Modifier.size(32.dp),
                                )
                            }
                            // 相机角标
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .size(26.dp)
                                    .clip(CircleShape)
                                    .background(LimePrimary.copy(alpha = 0.85f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    Icons.Default.CameraAlt,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp),
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(24.dp))

                    // 名字、背景图
                    FormCard {
                        FormRow(label = "名字", value = form.nickname, onClick = { showNicknameDialog = true })
                        HorizontalDivider(modifier = Modifier.padding(start = 16.dp), color = LimeLightGray)
                        // 背景图行
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { bgLauncher.launch("image/*") }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("背景图", style = MaterialTheme.typography.bodyLarge, color = LimeDark)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (form.backgroundUrl != null) {
                                    AsyncImage(
                                        model = form.backgroundUrl,
                                        contentDescription = "背景图预览",
                                        modifier = Modifier
                                            .size(width = 48.dp, height = 32.dp)
                                            .clip(RoundedCornerShape(4.dp)),
                                        contentScale = ContentScale.Crop,
                                    )
                                    Spacer(Modifier.width(8.dp))
                                }
                                Icon(
                                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = null,
                                    tint = LimeGray,
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // 简介
                    FormCard {
                        FormRow(
                            label = "简介",
                            value = form.bio.ifBlank { "填写简介" },
                            valueColor = if (form.bio.isBlank()) LimeGray else LimeDark,
                            onClick = { showBioDialog = true },
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    // 性别、生日、地区
                    FormCard {
                        val gender = when (form.gender) { 1 -> "男"; 2 -> "女"; else -> "未设置" }
                        FormRow(label = "性别", value = gender, onClick = { showGenderDialog = true })
                        HorizontalDivider(modifier = Modifier.padding(start = 16.dp), color = LimeLightGray)
                        FormRow(
                            label = "生日",
                            value = form.birthday.ifBlank { "未设置" },
                            valueColor = if (form.birthday.isBlank()) LimeGray else LimeDark,
                            onClick = { showBirthdayPicker = true },
                        )
                        HorizontalDivider(modifier = Modifier.padding(start = 16.dp), color = LimeLightGray)
                        FormRow(
                            label = "地区",
                            value = form.region.ifBlank { "未设置" },
                            valueColor = if (form.region.isBlank()) LimeGray else LimeDark,
                            onClick = { showRegionDialog = true },
                        )
                    }

                    if (state.error != null) {
                        Spacer(Modifier.height(12.dp))
                        Text(
                            state.error,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(horizontal = 16.dp),
                        )
                    }

                    if (state.isSaving) {
                        Spacer(Modifier.height(16.dp))
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = LimePrimary, modifier = Modifier.size(24.dp))
                        }
                    }

                    Spacer(Modifier.height(24.dp))
                }

                // 昵称编辑对话框
                if (showNicknameDialog) {
                    var draft by remember { mutableStateOf(form.nickname) }
                    AlertDialog(
                        onDismissRequest = { showNicknameDialog = false },
                        title = { Text("修改名字") },
                        text = {
                            OutlinedTextField(
                                value = draft,
                                onValueChange = { if (it.length <= 20) draft = it },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = LimePrimary),
                            )
                        },
                        confirmButton = {
                            TextButton(onClick = { viewModel.onNicknameChange(draft); showNicknameDialog = false }) {
                                Text("确定", color = LimePrimary)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showNicknameDialog = false }) { Text("取消") }
                        },
                    )
                }

                // 简介编辑对话框
                if (showBioDialog) {
                    var draft by remember { mutableStateOf(form.bio) }
                    AlertDialog(
                        onDismissRequest = { showBioDialog = false },
                        title = { Text("编辑简介") },
                        text = {
                            OutlinedTextField(
                                value = draft,
                                onValueChange = { if (it.length <= 200) draft = it },
                                minLines = 3,
                                maxLines = 5,
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = LimePrimary),
                            )
                        },
                        confirmButton = {
                            TextButton(onClick = { viewModel.onBioChange(draft); showBioDialog = false }) {
                                Text("确定", color = LimePrimary)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showBioDialog = false }) { Text("取消") }
                        },
                    )
                }

                // 性别选择对话框
                if (showGenderDialog) {
                    AlertDialog(
                        onDismissRequest = { showGenderDialog = false },
                        title = { Text("选择性别") },
                        text = {
                            Column {
                                listOf(Triple(0, "未设置", form.gender == 0),
                                    Triple(1, "男", form.gender == 1),
                                    Triple(2, "女", form.gender == 2)).forEach { (value, label, selected) ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { viewModel.onGenderChange(value); showGenderDialog = false }
                                            .padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        RadioButton(
                                            selected = selected,
                                            onClick = { viewModel.onGenderChange(value); showGenderDialog = false },
                                            colors = RadioButtonDefaults.colors(selectedColor = LimePrimary),
                                        )
                                        Text(label, modifier = Modifier.padding(start = 4.dp))
                                    }
                                }
                            }
                        },
                        confirmButton = {},
                        dismissButton = {
                            TextButton(onClick = { showGenderDialog = false }) { Text("取消") }
                        },
                    )
                }

                // 生日滚轮选择器
                if (showBirthdayPicker) {
                    WheelDatePicker(
                        initialDate = form.birthday,
                        onDismiss = { showBirthdayPicker = false },
                        onConfirm = { date: String ->
                            viewModel.onBirthdayChange(date)
                            showBirthdayPicker = false
                        },
                    )
                }

                // 地区编辑对话框
                if (showRegionDialog) {
                    var draft by remember { mutableStateOf(form.region) }
                    AlertDialog(
                        onDismissRequest = { showRegionDialog = false },
                        title = { Text("填写地区") },
                        text = {
                            OutlinedTextField(
                                value = draft,
                                onValueChange = { if (it.length <= 50) draft = it },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = LimePrimary),
                            )
                        },
                        confirmButton = {
                            TextButton(onClick = { viewModel.onRegionChange(draft); showRegionDialog = false }) {
                                Text("确定", color = LimePrimary)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showRegionDialog = false }) { Text("取消") }
                        },
                    )
                }
            }
        }
    }
}

/// 列表卡片部分
@Composable
private fun FormCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column { content() }
    }
}

/// 列表行部分
@Composable
private fun FormRow(
    label: String,
    value: String,
    valueColor: Color = LimeDark,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, color = LimeDark)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                value,
                style = MaterialTheme.typography.bodyMedium,
                color = valueColor,
                maxLines = 1,
            )
            Spacer(Modifier.width(4.dp))
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = LimeGray)
        }
    }
}
