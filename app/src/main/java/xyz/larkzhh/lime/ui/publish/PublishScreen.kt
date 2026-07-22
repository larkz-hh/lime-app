package xyz.larkzhh.lime.ui.publish

import android.net.Uri
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import coil3.compose.AsyncImage
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import xyz.larkzhh.lime.navigation.Screen
import xyz.larkzhh.lime.ui.components.LimeAlertDialog
import xyz.larkzhh.lime.ui.publish.viewmodel.PublishViewModel
import xyz.larkzhh.lime.ui.theme.LimePrimary
import xyz.larkzhh.lime.ui.theme.LimeWhite
import xyz.larkzhh.lime.util.showToast

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PublishScreen(
    navController: NavHostController,
    viewModel: PublishViewModel,
) {
    val publishState by viewModel.publishState.collectAsState()
    val context = LocalContext.current
    var showDraftDialog by remember { mutableStateOf(false) }

    // 发布成功后返回首页
    LaunchedEffect(publishState.isSuccess) {
        if (publishState.isSuccess) {
            viewModel.clearSuccess()
            navController.navigate(Screen.Home.route) {
                popUpTo(Screen.Home.route) { inclusive = false }
            }
        }
    }

    // 存草稿成功后显示 Toast 并返回首页
    LaunchedEffect(publishState.isDraftSuccess) {
        if (publishState.isDraftSuccess) {
            viewModel.clearDraftSuccess()
            "存草稿成功".showToast(context)
            navController.navigate(Screen.Home.route) {
                popUpTo(Screen.Home.route) { inclusive = false }
            }
        }
    }

    // 存草稿确认弹窗
    if (showDraftDialog) {
        LimeAlertDialog(
            title = "确认保存笔记至草稿箱吗？",
            onFirstButtonClick = { showDraftDialog = false },
            onSecondButtonClick = {
                showDraftDialog = false
                viewModel.saveDraft()
            },
            onDismissRequest = { showDraftDialog = false },
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        // 顶栏
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
            }
            Text(
                text = "发布笔记",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
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

            //HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            // 标题输入
            OutlinedTextField(
                value = publishState.title,
                onValueChange = viewModel::onTitleChange,
                placeholder = {
                    Text(
                        "添加标题",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.32f),
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                maxLines = 2,
                textStyle = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = Color.Transparent,
                    focusedBorderColor = Color.Transparent,
                ),
            )

            //HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            // 正文输入
            OutlinedTextField(
                value = publishState.content,
                onValueChange = viewModel::onContentChange,
                placeholder = {
                    Text(
                        "添加正文",
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.24f),
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 140.dp)
                    .padding(horizontal = 8.dp),
                maxLines = 20,
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = Color.Transparent,
                    focusedBorderColor = Color.Transparent,
                ),
            )
        }

        // 错误提示
        if (publishState.error != null) {
            Text(
                text = publishState.error!!,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
        }

        // 上传进度提示
        if (publishState.isPublishing) {
            val total = publishState.selectedUris.size
            val done = publishState.publishProgress
            if (done < total) {
                Text(
                    text = "正在上传图片 $done/$total...",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
        }

        // 底部栏
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // 存草稿
            Button(
                onClick = { showDraftDialog = true },
                enabled = !publishState.isPublishing,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = LimeWhite,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    disabledContainerColor = Color.White.copy(alpha = 0.6f),
                    disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
            ) {
                Text("存草稿", fontWeight = FontWeight.SemiBold)
            }
            // 发布笔记
            Button(
                onClick = { viewModel.publish() },
                enabled = !publishState.isPublishing,
                modifier = Modifier.weight(2f),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(containerColor = LimePrimary),
            ) {
                if (publishState.isPublishing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = Color.White,
                    )
                } else {
                    Text("发布笔记", fontWeight = FontWeight.SemiBold)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
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
