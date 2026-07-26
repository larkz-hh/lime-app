package xyz.larkzhh.lime.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import xyz.larkzhh.lime.data.network.model.NoteDetailData
import xyz.larkzhh.lime.ui.detail.components.AuthorBar
import xyz.larkzhh.lime.ui.detail.components.NoteBottomBar
import xyz.larkzhh.lime.ui.detail.components.NoteImagePager
import xyz.larkzhh.lime.ui.theme.LimeDark
import xyz.larkzhh.lime.ui.theme.LimeGray
import xyz.larkzhh.lime.ui.theme.LimeLightGray
import xyz.larkzhh.lime.ui.theme.LimePrimary

@Composable
fun DetailScreen(
    navController: NavHostController,
    noteId: String,
    viewModel: DetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(noteId) {
        viewModel.loadNote(noteId.toLongOrNull() ?: return@LaunchedEffect)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = LimePrimary)
                }
            }

            uiState.error != null -> {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = uiState.error ?: "加载失败",
                        color = LimeGray,
                        fontSize = 14.sp,
                    )
                }
            }

            uiState.note != null -> {
                AuthorBar(
                    note = uiState.note!!,
                    onBack = { navController.popBackStack() },
                )
                NoteContent(
                    note = uiState.note!!,
                    modifier = Modifier.weight(1f),
                )
                NoteBottomBar(
                    note = uiState.note!!,
                    onToggleLike = viewModel::toggleLike,
                    onToggleFavorite = viewModel::toggleFavorite,
                )
            }
        }
    }
}

@Composable
private fun NoteContent(
    note: NoteDetailData,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier.fillMaxWidth()) {
        // 图片轮播
        if (note.images.isNotEmpty()) {
            item {
                NoteImagePager(images = note.images)
            }
        }
        // 标题
        if (!note.title.isNullOrBlank()) {
            item {
                Text(
                    text = note.title,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = LimeDark,
                    ),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                )
            }
        }

        // 正文
        if (!note.content.isNullOrBlank()) {
            item {
                Text(
                    text = note.content,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = LimeDark,
                        lineHeight = 22.sp,
                    ),
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
        }

        // 更新时间
        item {
            Text(
                text = formatNoteTime(note.updateTime.orEmpty()),
                color = LimeGray,
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 16.dp, top = 10.dp, bottom = 16.dp),
            )
        }

        item { HorizontalDivider(color = LimeLightGray, thickness = 1.dp) }

        // 评论区
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "评论区施工中",
                    color = LimeGray,
                    fontSize = 13.sp,
                )
            }
        }
        item { Spacer(modifier = Modifier.height(72.dp)) }
    }
}

/// 格式化时间
private fun formatNoteTime(isoTime: String): String {
    return try {
        // eg:2026-07-20T10:30:00
        val date = isoTime.substringBefore("T")
        val parts = date.split("-")
        "${parts[0]}年${parts[1]}月${parts[2]}日"
    } catch (_: Exception) {
        isoTime
    }
}
