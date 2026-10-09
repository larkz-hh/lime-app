package xyz.larkzhh.lime.ui.qrscan

import android.Manifest
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Photo
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.launch
import xyz.larkzhh.lime.feature.qrscan.R
import xyz.larkzhh.lime.navigation.route.Screen
import xyz.larkzhh.lime.navigation.VideoOpener
import xyz.larkzhh.lime.ui.qrscan.components.CameraPreview
import xyz.larkzhh.lime.ui.qrscan.components.ScanOverlay
import xyz.larkzhh.lime.util.parseLimeNoteQr
import xyz.larkzhh.lime.util.parseLimeUserQr
import xyz.larkzhh.lime.util.parseLimeVideoQr
import xyz.larkzhh.lime.util.showToast
import xyz.larkzhh.lime.core.designsystem.R as DesignSystemR

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun QrScanScreen(navController: NavHostController) {
    val context = LocalContext.current
    val qrViewModel: QrScanViewModel = hiltViewModel()
    val scope = rememberCoroutineScope()
    val cameraPermission = rememberPermissionState(Manifest.permission.CAMERA)
    val scanner = remember { BarcodeScanning.getClient() }
    var isAlbumScanning by remember { mutableStateOf(false) }
    var camera by remember { mutableStateOf<Camera?>(null) }

    val userNotFoundText = stringResource(R.string.qr_user_not_found)
    val imageReadFailedText = stringResource(R.string.qr_image_read_failed)
    val noQrDetectedText = stringResource(R.string.qr_none_detected)
    val scanFailedText = stringResource(R.string.qr_scan_failed)

    DisposableEffect(Unit) {
        onDispose {
            scanner.close()
            camera?.cameraControl?.enableTorch(false)
        }
    }

    LaunchedEffect(Unit) {
        if (!cameraPermission.status.isGranted) {
            cameraPermission.launchPermissionRequest()
        }
    }

    fun handleResult(raw: String) {
        parseLimeUserQr(raw)?.let { identifier ->
            scope.launch {
                val userId = qrViewModel.resolveUserId(identifier)
                if (userId != null) {
                    navController.popBackStack()
                    navController.navigate(Screen.UserProfile.createRoute(userId))
                } else {
                    userNotFoundText.showToast(context)
                }
            }
            return
        }
        parseLimeVideoQr(raw)?.let { noteId ->
            navController.popBackStack()
            noteId.toLongOrNull()?.let { id ->
                if (navController.graph.findNode(Screen.VideoFeed.ROUTE) != null) {
                    navController.navigate(
                        Screen.VideoFeed.createRoute(id, Screen.VideoFeed.SOURCE_RECOMMENDATION)
                    )
                } else {
                    VideoOpener.open(
                        context,
                        id, Screen.VideoFeed.SOURCE_RECOMMENDATION)
                }
            }
            return
        }
        navController.popBackStack()
        parseLimeNoteQr(raw)?.let { noteId ->
            if (noteId.isNotBlank()) navController.navigate(Screen.Detail.createRoute(noteId))
            return
        }
        val uri = runCatching { raw.toUri() }.getOrNull()
        when (uri?.scheme) {
            "http", "https" -> {
                runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, uri)) }
            }
            else -> raw.showToast(context, Toast.LENGTH_LONG)
        }
    }

    val albumLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { imageUri ->
        if (imageUri == null) return@rememberLauncherForActivityResult
        val image: InputImage = try {
            InputImage.fromFilePath(context, imageUri)
        } catch (e: Exception) {
            imageReadFailedText.showToast(context)
            return@rememberLauncherForActivityResult
        }
        isAlbumScanning = true
        scanner.process(image)
            .addOnSuccessListener { barcodes ->
                isAlbumScanning = false
                val raw = barcodes.firstOrNull { it.format == Barcode.FORMAT_QR_CODE }?.rawValue
                if (!raw.isNullOrBlank()) handleResult(raw)
                else noQrDetectedText.showToast(context)
            }
            .addOnFailureListener {
                isAlbumScanning = false
                scanFailedText.showToast(context)
            }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        if (cameraPermission.status.isGranted) {
            CameraPreview(
                scanner = scanner,
                onCameraReady = { camera = it },
                onResult = ::handleResult,
            )
            ScanOverlay(camera = camera)
        } else {
            Text(
                text = stringResource(R.string.qr_permission_required),
                color = Color.White,
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.Center).padding(32.dp),
            )
        }

        // 相册扫描加载指示器
        if (isAlbumScanning) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = Color.White)
            }
        }

        // 顶部栏
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 48.dp, start = 8.dp, end = 8.dp),
        ) {
            IconButton(
                onClick = { navController.popBackStack() },
                modifier = Modifier.align(Alignment.TopStart),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(DesignSystemR.string.back),
                    tint = Color.White,
                )
            }
            Text(
                text = stringResource(R.string.qr_title),
                color = Color.White,
                fontSize = 18.sp,
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 12.dp),
            )
        }

        // 底部相册按钮
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 80.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            IconButton(
                onClick = {
                    albumLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                modifier = Modifier
                    .size(60.dp)
                    .background(Color.White.copy(alpha = 0.2f), CircleShape),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Photo,
                    contentDescription = stringResource(R.string.qr_album),
                    tint = Color.White,
                    modifier = Modifier.size(28.dp),
                )
            }
            Text(
                text = stringResource(R.string.qr_album),
                color = Color.White,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}