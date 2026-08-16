package xyz.larkzhh.lime.ui.search

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import xyz.larkzhh.lime.data.network.model.HotSearchItem
import xyz.larkzhh.lime.ui.components.LimeAlertDialog
import xyz.larkzhh.lime.ui.theme.LimeGray
import xyz.larkzhh.lime.ui.theme.LimeLightGray
import xyz.larkzhh.lime.ui.theme.LimePrimary

/// 历史记录收起时最多展示行数
private const val HISTORY_COLLAPSED_ROWS = 2

/// 历史记录展开时最多展示行数
private const val HISTORY_EXPANDED_ROWS = 6

@Composable
fun SearchHomeContent(
    history: List<String>,
    hotWords: List<HotSearchItem>,
    onKeywordClick: (String) -> Unit,
    onKeywordRemove: (String) -> Unit,
    onClearHistory: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showClearDialog by remember { mutableStateOf(false) }
    // 编辑模式
    var editing by remember { mutableStateOf(false) }
    // 历史删空后自动退出编辑模式
    LaunchedEffect(history) {
        if (history.isEmpty()) editing = false
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        if (history.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            // 历史记录标题栏
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "历史记录",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Spacer(modifier = Modifier.weight(1f))
                if (editing) {
                    // 编辑模式
                    Text(
                        text = "全部删除",
                        fontSize = 14.sp,
                        color = LimeGray,
                        modifier = Modifier.clickable { showClearDialog = true },
                    )
                    Text(
                        text = "完成",
                        fontSize = 14.sp,
                        color = LimeGray,
                        modifier = Modifier
                            .padding(start = 20.dp)
                            .clickable { editing = false },
                    )
                } else {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = "管理历史",
                        tint = LimeGray,
                        modifier = Modifier
                            .size(20.dp)
                            .clickable { editing = true },
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            HistoryFlow(
                history = history,
                editing = editing,
                onKeywordClick = onKeywordClick,
                onKeywordRemove = onKeywordRemove,
            )
        }

        if (hotWords.isNotEmpty()) {
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "热搜",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(modifier = Modifier.height(4.dp))
            HotSearchList(hotWords = hotWords, onKeywordClick = onKeywordClick)
        }
    }

    if (showClearDialog) {
        LimeAlertDialog(
            title = "确认清空全部搜索历史吗？",
            onFirstButtonClick = { showClearDialog = false },
            onSecondButtonClick = {
                showClearDialog = false
                onClearHistory()
            },
            onDismissRequest = { showClearDialog = false },
        )
    }
}

///历史记录卡片流
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HistoryFlow(
    history: List<String>,
    editing: Boolean,
    onKeywordClick: (String) -> Unit,
    onKeywordRemove: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val textMeasurer = rememberTextMeasurer()
    val textStyle = TextStyle(fontSize = 14.sp)
    val density = LocalDensity.current

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val containerWidthPx = constraints.maxWidth
        val chipHorizontalPaddingPx = with(density) { 24.dp.toPx() }
        val spacingPx = with(density) { 8.dp.toPx() }
        val toggleWidthPx = with(density) { 32.dp.toPx() }
        val maxRows = if (expanded) HISTORY_EXPANDED_ROWS else HISTORY_COLLAPSED_ROWS

        // 行数限制里的词条子集
        val visibleKeywords = remember(history, expanded, editing, containerWidthPx) {
            if (editing) return@remember history// 编辑模式下全部渲染
            if (containerWidthPx <= 0) return@remember emptyList<String>()
            val containerW = containerWidthPx.toFloat()
            val halfW = containerW / 2f
            // 词条芯片渲染宽度
            val widths = history.map { keyword ->
                minOf(
                    textMeasurer.measure(keyword, textStyle).size.width + chipHorizontalPaddingPx,// 文字宽+水平边距
                    halfW,// 容器一半
                )
            }
            // 模拟折行
            val placed = mutableListOf<Int>()
            var row = 0
            var x = 0f
            for (i in widths.indices) {
                val w = widths[i]
                if (x > 0f && x + w > containerW) { row++; x = 0f }
                if (row >= maxRows) break
                placed.add(i)// 存入索引
                x += w + spacingPx// 当前行的已用宽度
            }
            // 钮预留展开/收起位置
            while (placed.isNotEmpty()) {
                var toggleRow = row
                if (x + toggleWidthPx > containerW) toggleRow++// 按钮换行
                if (toggleRow <= maxRows - 1) break
                placed.removeAt(placed.lastIndex)
                row = 0; x = 0f
                // 重新计算剩余词条排版
                for (i in placed) {
                    val w = widths[i]
                    if (x > 0f && x + w > containerW) { row++; x = 0f }
                    x += w + spacingPx
                }
            }
            placed.map { history[it] }
        }

        // 词条芯片最大宽度
        val chipMaxWidth = with(density) { (containerWidthPx / 2).toDp() }

        Column(modifier = Modifier.fillMaxWidth()) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                visibleKeywords.forEach { keyword ->
                    Surface(
                        // 编辑模式下点芯片不能搜索
                        onClick = { if (!editing) onKeywordClick(keyword) },
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, LimeLightGray),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .widthIn(max = chipMaxWidth)
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                        ) {
                            Text(
                                text = keyword,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onBackground,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false), // 文字超出时收缩
                            )
                            // 编辑模式下的删除键
                            if (editing) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "删除",
                                    tint = LimeGray,
                                    modifier = Modifier
                                        .padding(start = 4.dp)
                                        .size(14.dp)
                                        .clickable { onKeywordRemove(keyword) },
                                )
                            }
                        }
                    }
                }
                // 展开/收起按钮，编辑模式下或不到两行时隐藏
                if (!editing && (expanded || visibleKeywords.size < history.size)) {
                    Surface(
                        onClick = { expanded = !expanded },
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, LimeLightGray),
                    ) {
                        Icon(
                            imageVector = if (expanded) Icons.Outlined.KeyboardArrowUp
                            else Icons.Outlined.KeyboardArrowDown,
                            contentDescription = if (expanded) "收起" else "展开",
                            tint = LimeGray,
                            modifier = Modifier
                                .size(32.dp)
                                .padding(5.dp),
                        )
                    }
                }
            }
        }
    }
}

/// 热搜榜
@Composable
private fun HotSearchList(
    hotWords: List<HotSearchItem>,
    onKeywordClick: (String) -> Unit,
) {
    val rows = hotWords.chunked(2)
    Column(modifier = Modifier.fillMaxWidth()) {
        rows.forEachIndexed { rowIndex, rowItems ->
            Row(modifier = Modifier.fillMaxWidth()) {
                rowItems.forEachIndexed { columnIndex, item ->
                    val rank = rowIndex * 2 + columnIndex + 1
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onKeywordClick(item.keyword) }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = rank.toString(),
                            fontSize = 14.sp,
                            color = if (rank <= 3) LimePrimary else LimeGray,
                            modifier = Modifier.widthIn(min = 24.dp),
                        )
                        Text(
                            text = item.keyword,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                // 奇数个词条时补齐第二列
                if (rowItems.size == 1) Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}