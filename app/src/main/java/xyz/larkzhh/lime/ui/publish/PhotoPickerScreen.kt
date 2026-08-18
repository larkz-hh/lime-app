package xyz.larkzhh.lime.ui.publish

import android.Manifest
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.video.videoFrameMillis
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.accompanist.permissions.rememberPermissionState
import xyz.larkzhh.lime.navigation.Screen
import xyz.larkzhh.lime.ui.components.ImageGridItem
import xyz.larkzhh.lime.ui.publish.viewmodel.LocalVideo
import xyz.larkzhh.lime.ui.publish.viewmodel.PublishViewModel
import xyz.larkzhh.lime.ui.publish.viewmodel.VideoPublishViewModel
import xyz.larkzhh.lime.ui.theme.LimePrimary
import xyz.larkzhh.lime.util.showToast

private enum class PickerTab { PHOTO, VIDEO }

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PhotoPickerScreen(
    navController: NavHostController,
    viewModel: PublishViewModel,
    videoViewModel: VideoPublishViewModel,
) {
    val pickerState by viewModel.pickerState.collectAsState()
    val videoPickerState by videoViewModel.pickerState.collectAsState()
    val context = LocalContext.current

    var tab by remember { mutableStateOf(PickerTab.PHOTO) }

    // 权限申请
    val granted: Boolean
    val requestPermission: () -> Unit
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val state = rememberMultiplePermissionsState(
            listOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO)
        )
        granted = state.allPermissionsGranted
        requestPermission = { state.launchMultiplePermissionRequest() }
    } else {
        val state = rememberPermissionState(Manifest.permission.READ_EXTERNAL_STORAGE)
        granted = state.status.isGranted
        requestPermission = { state.launchPermissionRequest() }
    }

    LaunchedEffect(granted) {
        if (granted) {
            viewModel.loadDeviceImages()
        } else {
            requestPermission()
        }
    }

    // 切到视频tab时懒加载视频
    LaunchedEffect(tab, granted) {
        if (granted && tab == PickerTab.VIDEO && videoPickerState.videos.isEmpty()) {
            videoViewModel.loadDeviceVideos()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        // 顶部栏
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.Filled.Close, contentDescription = "关闭")
            }
            Text(
                text = "选择",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            if (tab == PickerTab.PHOTO) {
                val count = pickerState.selectedUris.size
                if (count > 0) {
                    Text(
                        text = "已选 $count/9",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(end = 16.dp),
                    )
                }
            }
        }

        // 照片、视频 tab
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 4.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            PickerTabItem("照片", tab == PickerTab.PHOTO) { tab = PickerTab.PHOTO }
            PickerTabItem("视频", tab == PickerTab.VIDEO) { tab = PickerTab.VIDEO }
        }

        when {
            !granted -> {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("需要相册访问权限才能选择")
                        Button(
                            onClick = requestPermission,
                            modifier = Modifier.padding(top = 12.dp),
                        ) {
                            Text("授予权限")
                        }
                    }
                }
            }

            tab == PickerTab.PHOTO -> {
                if (pickerState.isLoading) {
                    LoadingBox()
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        items(pickerState.images, key = { it.id }) { image ->
                            ImageGridItem(
                                uri = image.uri,
                                selectionIndex = pickerState.selectedUris.indexOf(image.uri),
                                onToggle = { viewModel.toggleImageSelection(image.uri) },
                            )
                        }
                    }
                }
            }

            else -> {
                if (videoPickerState.isLoading) {
                    LoadingBox()
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        items(videoPickerState.videos, key = { it.id }) { video ->
                            VideoGridItem(
                                video = video,
                                isSelected = videoPickerState.selectedVideo?.id == video.id,
                                onClick = {
                                    if (video.selectable) {
                                        videoViewModel.selectVideo(video)
                                    } else {
                                        "视频需为 mp4，且不超过 200MB、10 分钟".showToast(context)
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }

        // 底部栏
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.End,
        ) {
            if (tab == PickerTab.PHOTO) {
                val count = pickerState.selectedUris.size
                NextButton(
                    text = if (count > 0) "下一步($count)" else "下一步",
                    enabled = count > 0,
                ) {
                    viewModel.confirmSelection()
                    navController.navigate(Screen.NotePublish.route)
                }
            } else {
                NextButton(
                    text = "下一步",
                    enabled = videoPickerState.selectedVideo != null,
                ) {
                    navController.navigate(Screen.VideoPublish.route)
                }
            }
        }
    }
}

@Composable
private fun PickerTabItem(text: String, selected: Boolean, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) MaterialTheme.colorScheme.onSurface
            else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Box(
            modifier = Modifier
                .padding(top = 2.dp)
                .height(2.dp)
                .width(20.dp)
                .background(if (selected) LimePrimary else Color.Transparent)
        )
    }
}

@Composable
private fun LoadingBox() {
    Box(
        modifier = Modifier
            .fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun NextButton(text: String, enabled: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(20.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
    ) {
        Text(text = text, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun VideoGridItem(
    video: LocalVideo,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val context = LocalContext.current
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clickable(onClick = onClick),
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(video.uri)
                .videoFrameMillis(0)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        // 不可选置灰
        if (!video.selectable) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White.copy(alpha = 0.55f))
            )
        } else if (isSelected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.25f))
            )
        }
        // 时长角标
        Text(
            text = formatDuration(video.durationMs),
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(4.dp),
        )
        // 选中标记
        if (isSelected && video.selectable) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(5.dp)
                    .size(24.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = "已选",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

/// 毫秒转 mm:ss
private fun formatDuration(ms: Long): String {
    val totalSec = ms / 1000
    val m = totalSec / 60
    val s = totalSec % 60
    return "%d:%02d".format(m, s)
}
