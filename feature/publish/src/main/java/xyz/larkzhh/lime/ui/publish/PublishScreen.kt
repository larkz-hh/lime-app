package xyz.larkzhh.lime.ui.publish

import android.net.Uri
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import xyz.larkzhh.lime.feature.publish.R
import xyz.larkzhh.lime.core.designsystem.R as DesignSystemR
import xyz.larkzhh.lime.navigation.state.PendingNoteEdit
import xyz.larkzhh.lime.navigation.route.Screen
import xyz.larkzhh.lime.ui.publish.ai.AiWriteImage
import xyz.larkzhh.lime.ui.publish.ai.AiWriteSheet
import xyz.larkzhh.lime.ui.publish.ai.AiWriteViewModel
import xyz.larkzhh.lime.ui.publish.components.NotePublishScaffold
import xyz.larkzhh.lime.ui.publish.viewmodel.PublishViewModel

@Composable
fun PublishScreen(
    navController: NavHostController,
    viewModel: PublishViewModel,
    onDraftSaved: (() -> Unit)? = null,
) {
    val publishState by viewModel.publishState.collectAsState()
    val aiViewModel: AiWriteViewModel = hiltViewModel()
    var showAiSheet by remember { mutableStateOf(false) }

    val isEdit = publishState.editingNoteId != null
    // 编辑入口
    LaunchedEffect(Unit) {
        val id = PendingNoteEdit.noteId ?: return@LaunchedEffect
        if (!PendingNoteEdit.isVideo) {
            PendingNoteEdit.noteId = null
            PendingNoteEdit.isVideo = false
            viewModel.startEdit(id)
        }
    }
    // 本地新选的图片
    val localUris = publishState.images.filter { !it.remote }.map { it.uri }
    val total = publishState.images.size
    val localTotal = localUris.size
    val progressText = if (publishState.isPublishing && publishState.publishProgress < localTotal) {
        stringResource(R.string.publish_uploading_images, publishState.publishProgress, localTotal)
    } else null
    // AI 可用图
    val aiImages: List<AiWriteImage> = publishState.images.map {
        if (it.remote) AiWriteImage(remoteUrl = it.uri.toString())
        else AiWriteImage(uri = it.uri)
    }

    // 编辑模式
    LaunchedEffect(Unit) {
        val savedStateHandle = navController.currentBackStackEntry?.savedStateHandle ?: return@LaunchedEffect
        savedStateHandle.getStateFlow<List<Uri>?>("comment_images", null).collect { uris ->
            if (!uris.isNullOrEmpty()) {
                viewModel.appendLocalImages(uris)
                savedStateHandle.remove<List<Uri>>("comment_images")
            }
        }
    }

    if (publishState.isLoadingEdit) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator()
        }
        return
    }

    NotePublishScaffold(
        navController = navController,
        topBarTitle = if (isEdit) stringResource(R.string.publish_title_edit_note) else stringResource(R.string.publish_title_new_note),
        title = publishState.title,
        content = publishState.content,
        onTitleChange = viewModel::onTitleChange,
        onContentChange = viewModel::onContentChange,
        isPublishing = publishState.isPublishing,
        error = publishState.error,
        progressText = progressText,
        isSuccess = publishState.isSuccess,
        isDraftSuccess = publishState.isDraftSuccess,
        onClearSuccess = viewModel::clearSuccess,
        onClearDraftSuccess = viewModel::clearDraftSuccess,
        onSaveDraft = viewModel::saveDraft,
        onPublish = viewModel::publish,
        onAiAssist = { showAiSheet = true },
        onAiAction = { action ->
            aiViewModel.start(action, publishState.content, aiImages)
            showAiSheet = true
        },
        hasImages = aiImages.isNotEmpty(),
        isEdit = isEdit,
        onDraftSaved = onDraftSaved,
    ) {
        // 图片横向列表
        val lazyListState = rememberLazyListState()
        val reorderState = rememberReorderableLazyListState(lazyListState) { from, to ->
            viewModel.reorderImages(from.index, to.index)
        }
        LazyRow(
            state = lazyListState,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            itemsIndexed(
                publishState.images,
                key = { _, image -> image.uri.toString() },
            ) { index, image ->
                ReorderableItem(reorderState, key = image.uri.toString()) { isDragging ->
                    val haptic = LocalHapticFeedback.current// 获取系统的触觉反馈服务实例
                    PublishImageItem(
                        uri = image.uri,
                        index = index,
                        isDragging = isDragging,
                        onRemove = { viewModel.removeImage(image.uri) },
                        modifier = Modifier.longPressDraggableHandle(
                            onDragStarted = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            },// 长按震动反馈
                        ),
                    )
                }
            }
            // 追加按钮（未满9张时显示）
            if (total < 9) {
                item {
                    Box(
                        modifier = Modifier
                            .size(90.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.03f))
                            .clickable {
                                if (isEdit) {
                                    navController.navigate(Screen.CommentPhotoPicker.route)
                                } else {
                                    viewModel.addMore()// 同步当前已选
                                    navController.popBackStack()// 返回
                                }
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Filled.Add,
                            contentDescription = stringResource(R.string.publish_add_image),
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.24f),
                        )
                    }
                }
            }
        }
    }

    // AI 帮写弹窗
    if (showAiSheet) {
        AiWriteSheet(
            content = publishState.content,
            images = aiImages,
            onApplyContent = viewModel::onContentChange,
            onApplyTitle = viewModel::onTitleChange,
            onDismiss = { showAiSheet = false },
            viewModel = aiViewModel,
        )
    }
}

@Composable
private fun PublishImageItem(
    uri: Uri,
    index: Int,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
    isDragging: Boolean = false,
) {
    val elevation by animateDpAsState(if (isDragging) 6.dp else 0.dp)
    Box(
        modifier = modifier
            .size(90.dp)
            .shadow(elevation, RoundedCornerShape(8.dp))
            .clip(RoundedCornerShape(8.dp))
    ) {
        AsyncImage(
            model = uri,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        // 左上角序号
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(4.dp)
                .size(18.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color.Black.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = (index + 1).toString(),
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        // 右上角删除按钮
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(3.dp)
                .size(18.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable(onClick = onRemove),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.Close,
                contentDescription = stringResource(DesignSystemR.string.delete),
                tint = Color.White,
                modifier = Modifier.size(12.dp),
            )
        }
    }
}
