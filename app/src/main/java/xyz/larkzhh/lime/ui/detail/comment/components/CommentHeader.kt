package xyz.larkzhh.lime.ui.detail.comment.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import xyz.larkzhh.lime.ui.detail.comment.viewmodel.CommentSort
import xyz.larkzhh.lime.ui.theme.LimeDark
import xyz.larkzhh.lime.ui.theme.LimeGray
import xyz.larkzhh.lime.ui.theme.LimePrimary
import xyz.larkzhh.lime.ui.theme.LimeWhite

@Composable
fun CommentHeader(
    commentCount: Int,
    sort: CommentSort,
    onSortChange: (CommentSort) -> Unit,
) {
    var showMenu by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = "共${commentCount}条评论",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = LimeDark,
        )
        Box {
            IconButton(
                onClick = { showMenu = true },
                modifier = Modifier.size(32.dp),
            ) {
                Icon(Icons.AutoMirrored.Outlined.Sort, contentDescription = "排序", tint = LimeGray, modifier = Modifier.size(20.dp))
            }
            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }, containerColor = LimeWhite) {
                DropdownMenuItem(
                    text = { Text("按热度", fontSize = 14.sp, color = if (sort == CommentSort.HOT) LimePrimary else LimeDark) },
                    onClick = { onSortChange(CommentSort.HOT); showMenu = false },
                )
                DropdownMenuItem(
                    text = { Text("按时间", fontSize = 14.sp, color = if (sort == CommentSort.TIME) LimePrimary else LimeDark) },
                    onClick = { onSortChange(CommentSort.TIME); showMenu = false },
                )
            }
        }
    }
}