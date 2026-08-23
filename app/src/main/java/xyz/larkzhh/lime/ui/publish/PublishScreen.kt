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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import coil3.compose.AsyncImage
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import xyz.larkzhh.lime.ui.publish.components.NotePublishScaffold
import xyz.larkzhh.lime.ui.publish.viewmodel.PublishViewModel

@Composable
fun PublishScreen(
    navController: NavHostController,
    viewModel: PublishViewModel,
) {
    val publishState by viewModel.publishState.collectAsState()

    val total = publishState.selectedUris.size
    val done = publishState.publishProgress
    val progressText = if (done < total) "正在上传图片 $done/$total..." else null

    NotePublishScaffold(
        navController = navController,
        topBarTitle = "发布笔记",
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
                publishState.selectedUris,
                key = { _, uri -> uri.toString() },
            ) { index, uri ->
                ReorderableItem(reorderState, key = uri.toString()) { isDragging ->
                    val haptic = LocalHapticFeedback.current// 获取系统的触觉反馈服务实例
                    PublishImageItem(
                        uri = uri,
                        index = index,
                        isDragging = isDragging,
                        onRemove = { viewModel.removeImage(uri) },
                        modifier = Modifier.longPressDraggableHandle(
                            onDragStarted = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            },// 长按震动反馈
                        ),
                    )
                }
            }
            // 追加按钮（未满9张时显示）
            if (publishState.selectedUris.size < 9) {
                item {
                    Box(
                        modifier = Modifier
                            .size(90.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.03f))
                            .clickable {
                                viewModel.addMore()// 同步当前已选
                                navController.popBackStack()// 返回
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Filled.Add,
                            contentDescription = "添加图片",
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.24f),
                        )
                    }
                }
            }
        }
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
                contentDescription = "删除",
                tint = Color.White,
                modifier = Modifier.size(12.dp),
            )
        }
    }
}
