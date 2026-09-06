package xyz.larkzhh.lime.ui.draft

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
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
import kotlinx.coroutines.flow.distinctUntilChanged
import xyz.larkzhh.lime.R
import xyz.larkzhh.lime.data.network.model.FeedItem
import xyz.larkzhh.lime.navigation.PendingNoteEdit
import xyz.larkzhh.lime.navigation.Screen
import xyz.larkzhh.lime.ui.components.ErrorState
import xyz.larkzhh.lime.ui.theme.LimeGray
import xyz.larkzhh.lime.ui.theme.LimeLightGray
import xyz.larkzhh.lime.ui.theme.LimeWhite
import xyz.larkzhh.lime.ui.video.components.formatTime
import xyz.larkzhh.lime.util.showToast

@Composable
fun DraftBoxScreen(
    navController: NavHostController,
    onClose: () -> Unit,
) {
    val viewModel: DraftBoxViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val gridState = rememberLazyGridState()
    val deleteFailedTemplate = stringResource(R.string.draft_delete_failed)

    // 删除失败提示
    LaunchedEffect(uiState.deleteError) {
        uiState.deleteError?.let {
            deleteFailedTemplate.replace("%1\$s", it).showToast(context)
            viewModel.clearDeleteError()
        }
    }

    // 刷新列表
    LaunchedEffect(Unit) {
        if (DraftListRefresh.pending) {
            DraftListRefresh.pending = false
            viewModel.refresh()
        }
    }


    LaunchedEffect(gridState) {
        snapshotFlow { gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 }
            .distinctUntilChanged()
            .collect { last ->
                if (last >= uiState.items.size - 6 && uiState.hasMore) {
                    viewModel.loadMore()
                }
            }
    }

    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            // 顶栏
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { if (!navController.popBackStack()) onClose() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                }
                Text(
                    text = if (uiState.isManaging) {
                        stringResource(R.string.draft_selected_title, uiState.selectedIds.size)
                    } else {
                        stringResource(R.string.draft_box_title)
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                if (uiState.items.isNotEmpty() && !uiState.isLoading) {
                    TextButton(
                        onClick = {
                            if (uiState.isManaging) viewModel.exitManageMode() else viewModel.enterManageMode()
                        },
                    ) {
                        Text(
                            if (uiState.isManaging) stringResource(R.string.draft_done)
                            else stringResource(R.string.draft_manage)
                        )
                    }
                }
            }

            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(28.dp),
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 2.dp,
                        )
                    }
                }
                uiState.error != null && uiState.items.isEmpty() -> {
                    ErrorState(
                        message = uiState.error,
                        onRetry = viewModel::refresh,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                uiState.items.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(text = stringResource(R.string.draft_empty), color = LimeGray, fontSize = 14.sp)
                    }
                }
                else -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        state = gridState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(
                            count = uiState.items.size,
                            key = { index -> uiState.items[index].id },
                        ) { index ->
                            val item = uiState.items[index]
                            DraftGridItem(
                                item = item,
                                isSelectMode = uiState.isManaging,
                                isSelected = item.id in uiState.selectedIds,
                                onClick = {
                                    if (uiState.isManaging) {
                                        viewModel.toggleSelect(item.id)
                                    } else {
                                        PendingNoteEdit.noteId = item.id
                                        PendingNoteEdit.isVideo = item.noteType == 2
                                        navController.navigate(
                                            if (item.noteType == 2) Screen.VideoPublish.route
                                            else Screen.NotePublish.route
                                        )
                                    }
                                },
                                onToggleSelect = { viewModel.toggleSelect(item.id) },
                            )
                        }
                        if (uiState.isLoadingMore) {
                            item(key = "loading_more") {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        color = MaterialTheme.colorScheme.primary,
                                        strokeWidth = 2.dp,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 管理模式底部删除栏
            if (uiState.isManaging) {
                Surface(
                    shadowElevation = 8.dp,
                    color = MaterialTheme.colorScheme.background,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(R.string.draft_selected_count, uiState.selectedIds.size),
                            color = LimeGray,
                            fontSize = 14.sp,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(
                            onClick = viewModel::deleteSelected,
                            enabled = uiState.selectedIds.isNotEmpty() && !uiState.isDeleting,
                        ) {
                            Text(
                                text = if (uiState.isDeleting) stringResource(R.string.draft_deleting)
                                else stringResource(R.string.delete),
                                color = Color(0xFFFF3B30),
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
            }
        }
    }
}

/// 草稿卡片
@Composable
private fun DraftGridItem(
    item: FeedItem,
    isSelectMode: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    onToggleSelect: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column {
            // 封面
            Box(modifier = Modifier.fillMaxWidth()) {
                if (item.coverImage != null) {
                    AsyncImage(
                        model = item.coverImage,
                        contentDescription = item.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(3f / 4f)
                            .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)),
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(3f / 4f)
                            .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = item.title?.take(4) ?: stringResource(R.string.draft_cover_placeholder),
                            color = LimeGray,
                            fontSize = 13.sp,
                        )
                    }
                }
                // 视频草稿
                if (item.noteType == 2) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.4f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Filled.PlayArrow,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    item.video?.durationMs?.let { ms ->
                        Text(
                            text = formatTime(ms),
                            color = Color.White,
                            fontSize = 11.sp,
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(5.dp),
                        )
                    }
                }
                // 管理模式勾选框
                if (isSelectMode) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                            .background(if (isSelected) Color.Black.copy(alpha = 0.25f) else Color.Transparent),
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Black.copy(alpha = 0.3f))
                            .border(
                                width = 1.5.dp,
                                color = if (isSelected) Color.Transparent else Color.White,
                                shape = CircleShape,
                            )
                            .clickable(onClick = onToggleSelect),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (isSelected) {
                            Icon(
                                Icons.Filled.Check,
                                contentDescription = stringResource(R.string.draft_selected),
                                tint = Color.White,
                                modifier = Modifier.size(14.dp),
                            )
                        }
                    }
                }
            }
            Text(
                text = item.title ?: stringResource(R.string.draft_unnamed),
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 13.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            )
        }
    }
}
