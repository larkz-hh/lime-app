package xyz.larkzhh.lime.ui.publish

import android.Manifest
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavHostController
import android.provider.MediaStore
import android.util.Size
import androidx.compose.foundation.Image
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.produceState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.withContext
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.accompanist.permissions.rememberPermissionState
import xyz.larkzhh.lime.R
import xyz.larkzhh.lime.navigation.Screen
import xyz.larkzhh.lime.ui.components.ImageGridItem
import xyz.larkzhh.lime.ui.publish.components.PickerImagePreview
import xyz.larkzhh.lime.ui.publish.components.PickerVideoPreview
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
    replaceMode: Boolean = false,// 更换视频模式
) {
    val pickerState by viewModel.pickerState.collectAsState()
    val videoPickerState by videoViewModel.pickerState.collectAsState()
    val context = LocalContext.current

    // 已有选中视频
    var tab by remember {
        mutableStateOf(
            if (videoPickerState.selectedVideo != null) PickerTab.VIDEO else PickerTab.PHOTO
        )
    }

    // 预览浮层
    var previewImageIndex by remember { mutableStateOf<Int?>(null) }
    var previewVideoIndex by remember { mutableStateOf<Int?>(null) }

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
            // 更换视频模式只选视频
            if (!replaceMode && pickerState.images.isEmpty()) {
                viewModel.loadDeviceImages()
            }
        } else {
            requestPermission()
        }
    }

    // 切到视频tab时懒加载视频
    LaunchedEffect(tab, granted, replaceMode) {
        val needVideo = replaceMode || tab == PickerTab.VIDEO
        if (granted && needVideo && videoPickerState.videos.isEmpty()) {
            videoViewModel.loadDeviceVideos()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding()
    ) {
        // 顶部栏
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black)
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.picker_close), tint = Color.White)
            }
            Text(
                text = if (replaceMode) stringResource(R.string.picker_title_replace_video)
                else stringResource(R.string.picker_title_select),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                modifier = Modifier.weight(1f),
            )
            if (tab == PickerTab.PHOTO) {
                val count = pickerState.selectedUris.size
                if (count > 0) {
                    Text(
                        text = stringResource(R.string.picker_selected_count, count),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(end = 16.dp),
                    )
                }
            }
        }

        // 照片、视频 tab
        if (!replaceMode) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black)
                    .padding(start = 16.dp, top = 4.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                PickerTabItem(stringResource(R.string.picker_tab_photo), tab == PickerTab.PHOTO) { tab = PickerTab.PHOTO }
                PickerTabItem(stringResource(R.string.picker_tab_video), tab == PickerTab.VIDEO) { tab = PickerTab.VIDEO }
            }
        }

        val showVideo = replaceMode || tab == PickerTab.VIDEO
        when {
            !granted -> {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(stringResource(R.string.picker_permission_required), color = Color.White)
                        Button(
                            onClick = requestPermission,
                            modifier = Modifier.padding(top = 12.dp),
                        ) {
                            Text(stringResource(R.string.picker_permission_grant))
                        }
                    }
                }
            }

            !showVideo -> {
                if (pickerState.isLoading) {
                    LoadingBox()
                } else {
                    val gridState = rememberLazyGridState()
                    LaunchedEffect(gridState) {
                        snapshotFlow {
                            gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                        }
                            .distinctUntilChanged()
                            .collect { lastIndex ->
                                if (lastIndex >= pickerState.images.size - 6 &&
                                    pickerState.hasMore && !pickerState.isLoadingMore
                                ) {
                                    viewModel.loadMoreImages()
                                }
                            }
                    }
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        state = gridState,
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        items(pickerState.images, key = { it.id }) { image ->
                            ImageGridItem(
                                uri = image.uri,
                                selectionIndex = pickerState.selectedUris.indexOf(image.uri),
                                onToggle = { viewModel.toggleImageSelection(image.uri) },
                                onPreview = {
                                    previewImageIndex = pickerState.images.indexOf(image)
                                },
                            )
                        }
                        if (pickerState.isLoadingMore) {
                            item(span = { GridItemSpan(3) }) {
                                LoadingBox()
                            }
                        }
                    }
                }
            }

            else -> {
                if (videoPickerState.isLoading) {
                    LoadingBox()
                } else {
                    val gridState = rememberLazyGridState()
                    LaunchedEffect(gridState) {
                        snapshotFlow {
                            gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                        }
                            .distinctUntilChanged()
                            .collect { lastIndex ->
                                if (lastIndex >= videoPickerState.videos.size - 6 &&
                                    videoPickerState.hasMore && !videoPickerState.isLoadingMore
                                ) {
                                    videoViewModel.loadMoreVideos()
                                }
                            }
                    }
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        state = gridState,
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        items(videoPickerState.videos, key = { it.id }) { video ->
                            val videoConstraintText = stringResource(R.string.picker_video_constraint)
                            VideoGridItem(
                                video = video,
                                isSelected = videoPickerState.selectedVideo?.id == video.id,
                                onPreview = {
                                    previewVideoIndex = videoPickerState.videos.indexOf(video)
                                },
                                onToggle = {
                                    if (video.selectable) {
                                        if (replaceMode) {
                                            // 点已选视频不变
                                            if (videoPickerState.selectedVideo?.uri != video.uri) {
                                                videoViewModel.selectVideo(video)
                                            }
                                        } else {
                                            videoViewModel.toggleVideoSelection(video)
                                        }
                                    } else {
                                        videoConstraintText.showToast(context)
                                    }
                                },
                            )
                        }
                        if (videoPickerState.isLoadingMore) {
                            item(span = { GridItemSpan(3) }) {
                                LoadingBox()
                            }
                        }
                    }
                }
            }
        }

        // 底部栏
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.End,
        ) {
            when {
                replaceMode -> {
                    NextButton(
                        text = stringResource(R.string.picker_done),
                        enabled = videoPickerState.selectedVideo != null,
                    ) {
                        navController.popBackStack()
                    }
                }
                tab == PickerTab.PHOTO -> {
                    val count = pickerState.selectedUris.size
                    NextButton(
                        text = if (count > 0) stringResource(R.string.picker_next_count, count)
                        else stringResource(R.string.picker_next),
                        enabled = count > 0,
                    ) {
                        viewModel.confirmSelection()
                        navController.navigate(Screen.NotePublish.route)
                    }
                }
                else -> {
                    NextButton(
                        text = stringResource(R.string.picker_next),
                        enabled = videoPickerState.selectedVideo != null,
                    ) {
                        navController.navigate(Screen.VideoPublish.route)
                    }
                }
            }
        }
    }

    // 图片预览浮层
    val imgIdx = previewImageIndex
    if (imgIdx != null && imgIdx in pickerState.images.indices) {
        Dialog(
            onDismissRequest = { previewImageIndex = null },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false,
            ),
        ) {
            PickerImagePreview(
                images = pickerState.images,
                initialIndex = imgIdx,
                selectedUris = pickerState.selectedUris,
                onToggle = { viewModel.toggleImageSelection(it) },
                onDismiss = { previewImageIndex = null },
            )
        }
    }

    // 视频预览浮层
    val vidIdx = previewVideoIndex
    if (vidIdx != null && vidIdx in videoPickerState.videos.indices) {
        Dialog(
            onDismissRequest = { previewVideoIndex = null },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false,
            ),
        ) {
            PickerVideoPreview(
                videos = videoPickerState.videos,
                initialIndex = vidIdx,
                selectedId = videoPickerState.selectedVideo?.id,
                onToggle = { video ->
                    if (replaceMode) {
                        // 点已选视频不变
                        if (videoPickerState.selectedVideo?.uri != video.uri) {
                            videoViewModel.selectVideo(video)
                        }
                    } else {
                        videoViewModel.toggleVideoSelection(video)
                    }
                },
                onDismiss = { previewVideoIndex = null },
            )
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
            color = if (selected) Color.White
            else Color.White.copy(alpha = 0.6f),
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
            disabledContainerColor = Color.White.copy(alpha = 0.12f),
            disabledContentColor = Color.White.copy(alpha = 0.6f),
        ),
    ) {
        Text(text = text, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun VideoGridItem(
    video: LocalVideo,
    isSelected: Boolean,
    onPreview: () -> Unit,
    onToggle: () -> Unit,
) {
    val context = LocalContext.current
    // 用系统视频缩略图
    val thumb by produceState<ImageBitmap?>(initialValue = null, video.uri) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    context.contentResolver.loadThumbnail(video.uri, Size(360, 360), null)
                } else {
                    @Suppress("DEPRECATION")
                    MediaStore.Video.Thumbnails.getThumbnail(
                        context.contentResolver,
                        video.id,
                        MediaStore.Video.Thumbnails.MINI_KIND,
                        null,
                    )
                }
            }.getOrNull()?.asImageBitmap()
        }
    }
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clickable(onClick = onPreview),
    ) {
        val bmp = thumb
        if (bmp != null) {
            Image(
                bitmap = bmp,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
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
        if (video.selectable) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(5.dp)
                    .size(24.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .then(
                        if (isSelected) {
                            Modifier.background(MaterialTheme.colorScheme.primary)
                        } else {
                            Modifier
                                .background(Color.Black.copy(alpha = 0.3f))
                                .border(2.dp, Color.White, RoundedCornerShape(12.dp))
                        }
                    )
                    .clickable(onClick = onToggle),
                contentAlignment = Alignment.Center,
            ) {
                if (isSelected) {
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = stringResource(R.string.picker_selected),
                        tint = Color.White,
                        modifier = Modifier.size(16.dp),
                    )
                }
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
