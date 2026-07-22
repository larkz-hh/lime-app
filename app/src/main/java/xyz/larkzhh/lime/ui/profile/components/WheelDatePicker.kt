package xyz.larkzhh.lime.ui.profile.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.abs
import xyz.larkzhh.lime.ui.theme.LimePrimary
import xyz.larkzhh.lime.ui.theme.LimeWhite

private val ITEM_H = 52.dp  // 每个选项项的高度
private const val VISIBLE = 7 // 可见行数（奇数）
private const val HALF = VISIBLE / 2 // 3

/**
 * 底部滚轮日期选择器
 * 年份：1900 ~ 今年
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WheelDatePicker(
    initialDate: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    val today = LocalDate.now()

    // 解析初始日期
    val (initY, initM, initD) = remember(initialDate) {
        runCatching {
            val p = initialDate.split("-")
            Triple(p[0].toInt(), p[1].toInt(), p[2].toInt())
        }.getOrDefault(Triple(today.year - 18, 1, 1))
    }

    var year  by remember { mutableIntStateOf(initY.coerceIn(1900, today.year)) }
    var month by remember { mutableIntStateOf(initM.coerceIn(1, 12)) }
    var day   by remember { mutableIntStateOf(initD.coerceIn(1, 31)) }

    // 当年限制月份上限
    val maxMonth = if (year == today.year) today.monthValue else 12

    // 当年当月限制日期上限
    val maxDay = runCatching {
        val raw = YearMonth.of(year, month).lengthOfMonth()// 计算月长度
        if (year == today.year && month == today.monthValue) minOf(raw, today.dayOfMonth) else raw
    }.getOrDefault(31)

    // 截掉超出值
    val displayMonth = month.coerceIn(1, maxMonth)
    val displayDay   = day.coerceIn(1, maxDay)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = LimeWhite,
    ) {
        Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 32.dp)) {

            // 顶部栏
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onDismiss) {
                    Text("取消", style = MaterialTheme.typography.bodyLarge)
                }
                Text(
                    text = "选择你的生日",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                )
                TextButton(onClick = {
                    onConfirm("%04d-%02d-%02d".format(year, displayMonth, displayDay))
                }) {
                    Text("保存", color = LimePrimary, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                }
            }

            // 滚轮白色圆角卡片
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(0.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // 年份滚轮
                    val years = remember { (1900..today.year).map { "${it}年" } }// 转成字符串列表
                    WheelColumn(
                        items = years,
                        startIndex = (year - 1900).coerceIn(0, years.lastIndex),
                        onIndexChanged = { year = it + 1900 },
                        modifier = Modifier.weight(3f),
                    )
                    // 月份滚轮，今年限制到当月
                    key(maxMonth) {
                        val months = remember(maxMonth) { (1..maxMonth).map { "%02d月".format(it) } }
                        WheelColumn(
                            items = months,
                            startIndex = (displayMonth - 1).coerceIn(0, months.lastIndex),
                            onIndexChanged = { month = it + 1 },
                            modifier = Modifier.weight(2f),
                        )
                    }
                    // 日期滚轮，随年或月变化重置
                    key(year, month) {
                        val days = remember(maxDay) { (1..maxDay).map { "%02d日".format(it) } }
                        WheelColumn(
                            items = days,
                            startIndex = (displayDay - 1).coerceIn(0, days.lastIndex),
                            onIndexChanged = { day = it + 1 },
                            modifier = Modifier.weight(2f),
                        )
                    }
                }
            }
        }
    }
}

/// 单列滚轮
@Composable
private fun WheelColumn(
    items: List<String>,
    startIndex: Int,
    onIndexChanged: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState   = rememberLazyListState(initialFirstVisibleItemIndex = startIndex)
    val snapBehavior = rememberSnapFlingBehavior(listState)// 滚动吸附

    val selected by remember { derivedStateOf { listState.firstVisibleItemIndex } }

    LaunchedEffect(selected) {
        if (selected in items.indices) onIndexChanged(selected)
    }

    Box(modifier = modifier.height(ITEM_H * VISIBLE)) {
        LazyColumn(
            state   = listState,
            flingBehavior = snapBehavior,
            contentPadding = PaddingValues(vertical = ITEM_H * HALF),// 总高度 ITEM_H * (items.size + 1)
            modifier = Modifier.fillMaxSize(),
        ) {
            itemsIndexed(items) { index, item ->
                val dist = abs(selected - index)// 前项与选中项的索引距离
                val alpha = when (dist) {
                    0 -> 1f
                    1 -> 0.5f
                    2 -> 0.25f
                    else -> 0.1f
                }
                Box(
                    modifier = Modifier.height(ITEM_H).fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = item,
                        style = if (dist == 0) MaterialTheme.typography.titleMedium
                                else MaterialTheme.typography.bodyMedium,
                        fontWeight = if (dist == 0) FontWeight.Bold else FontWeight.Normal,
                        color = Color(0xFF1A1A1A).copy(alpha = alpha),
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }

        // 选中项上下分隔线
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .height(ITEM_H),
        ) {
            HorizontalDivider(modifier = Modifier.align(Alignment.TopCenter),    color = Color(0xFFE0E0E0))
            HorizontalDivider(modifier = Modifier.align(Alignment.BottomCenter), color = Color(0xFFE0E0E0))
        }

        // 顶部渐变遮罩.白色淡入
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(ITEM_H * HALF)
                .background(Brush.verticalGradient(listOf(Color.White, Color.Transparent))),
        )
        // 底部渐变遮罩.白色淡出
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(ITEM_H * HALF)
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.White))),
        )
    }
}