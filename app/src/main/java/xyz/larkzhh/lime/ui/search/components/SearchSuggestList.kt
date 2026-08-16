package xyz.larkzhh.lime.ui.search.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.NorthWest
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import xyz.larkzhh.lime.ui.theme.LimeGray
import xyz.larkzhh.lime.ui.theme.LimeLightGray
import xyz.larkzhh.lime.ui.theme.LimePrimary

/**
 * 搜索联想列表
 * - 词条中和输入匹配的部分高亮；
 * - 箭头点击后把词条填入搜索框
 */
@Composable
fun SearchSuggestList(
    suggestions: List<String>,
    query: String,
    onSuggestionClick: (String) -> Unit,
    onFillQuery: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier.fillMaxSize()) {
        items(suggestions, key = { it }) { suggestion ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSuggestionClick(suggestion) }
                    .height(48.dp)
                    .padding(start = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = null,
                    tint = LimeGray,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = rememberHighlighted(suggestion, query),
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp),
                )
                // 补全按钮
                IconButton(onClick = { onFillQuery(suggestion) }) {
                    Icon(
                        imageVector = Icons.Outlined.NorthWest,
                        contentDescription = "补全",
                        tint = LimeGray,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
            HorizontalDivider(thickness = 0.5.dp, color = LimeLightGray)
        }
    }
}

/// 构造高亮文本
@Composable
private fun rememberHighlighted(suggestion: String, query: String) = remember(suggestion, query) {
    buildAnnotatedString {
        val keyword = query.trim()
        val start = if (keyword.isEmpty()) -1 else suggestion.indexOf(keyword, ignoreCase = true)
        if (start < 0) {
            append(suggestion)// 未匹配到
        } else {
            append(suggestion.substring(0, start))
            withStyle(SpanStyle(color = LimePrimary)) {
                append(suggestion.substring(start, start + keyword.length))
            }
            append(suggestion.substring(start + keyword.length))
        }
    }
}
