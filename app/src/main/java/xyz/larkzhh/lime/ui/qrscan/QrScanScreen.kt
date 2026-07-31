package xyz.larkzhh.lime.ui.qrscan

import android.Manifest
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Photo
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.navigation.NavHostController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import xyz.larkzhh.lime.navigation.Screen
import xyz.larkzhh.lime.ui.qrscan.components.CameraPreview
import xyz.larkzhh.lime.ui.qrscan.components.ScanOverlay
import xyz.larkzhh.lime.util.showToast

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun QrScanScreen(navController: NavHostController) {
    val context = LocalContext.current
    val cameraPermission = rememberPermissionState(Manifest.permission.CAMERA)

    LaunchedEffect(Unit) {
        if (!cameraPermission.status.isGranted) {
            cameraPermission.launchPermissionRequest()
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        if (cameraPermission.status.isGranted) {
            CameraPreview(onResult = { raw ->
                navController.popBackStack()
                val uri = runCatching { raw.toUri() }.getOrNull()
                when {
                    // 笔记页面
                    uri?.scheme == "lime" && uri.host == "note" -> {
                        val noteId = uri.pathSegments.firstOrNull().orEmpty()
                        if (noteId.isNotBlank()) {
                            navController.navigate(Screen.Detail.createRoute(noteId))
                        }
                    }
                    // 用户页面
                    uri?.scheme == "lime" && uri.host == "user" -> {
                        Toast.makeText(context, "施工中", Toast.LENGTH_SHORT).show()
                    }
                    uri?.scheme == "http" || uri?.scheme == "https" -> {
                        runCatching {
                            context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                        }
                    }
                    else -> {
                         raw.showToast(context, Toast.LENGTH_LONG)
                    }
                }
            })
            ScanOverlay()
        } else {
            Text(
                text = "需要相机权限才能扫描二维码",
                color = Color.White,
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.Center).padding(32.dp),
            )
        }

        // 顶部栏
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 48.dp, start = 8.dp, end = 8.dp),
        ) {
            IconButton(
                onClick = { navController.popBackStack() },
                modifier = Modifier.align(Alignment.TopStart)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "返回",
                    tint = Color.White,
                )
            }
            Text(
                text = "扫描二维码",
                color = Color.White,
                fontSize = 18.sp,
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 12.dp),
            )
        }

        // 底部相册按钮
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 80.dp),
        ) {
            IconButton(
                onClick = { /* TODO: 相册 */ },
                modifier = Modifier
                    .size(60.dp)
                    .background(Color.White.copy(alpha = 0.2f), CircleShape),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Photo,
                    contentDescription = "相册",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp),
                )
            }
            Text(
                text = "相册",
                color = Color.White,
                fontSize = 12.sp,
                modifier = Modifier.align(Alignment.BottomCenter).offset(y = 24.dp),
            )
        }
    }
}