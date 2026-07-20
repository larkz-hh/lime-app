package xyz.larkzhh.lime.ui.profile

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavHostController
import xyz.larkzhh.lime.ui.profile.components.ProfileHeader
import xyz.larkzhh.lime.ui.profile.viewmodel.ProfileViewModel
import xyz.larkzhh.lime.ui.theme.LimeLightGray

@Composable
fun ProfileScreen(navController: NavHostController) {
    val viewModel: ProfileViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsState()
    val uploadError by viewModel.uploadError.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    /// 从编辑页返回时重新拉取最新用户信息
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.loadUser()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    /// 选择图片上传头像
    val avatarPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> if (uri != null) viewModel.uploadAvatar(uri) }

    /// 错误弹窗提示
    LaunchedEffect(uploadError) {
        uploadError?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearUploadError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = LimeLightGray,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding())
        ) {
            ProfileHeader(
                uiState = uiState,
                onEditProfile = { navController.navigate("edit_profile") },
                onEditAvatar = { avatarPickerLauncher.launch("image/*") },
            )// 头部区域
        }
    }
}
