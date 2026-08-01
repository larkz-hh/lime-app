package xyz.larkzhh.lime.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import xyz.larkzhh.lime.data.network.model.FeedItem
import xyz.larkzhh.lime.ui.theme.LimePrimary

/**
 * 支持选择的笔记卡片
 *
 * @param item 信息流数据实体
 * @param liked 当前用户是否已点赞
 * @param onLikeToggle 点赞切换回调
 * @param onClick 卡片点击
 * @param modifier 外部传入的 Modifier
 * @param isSelectMode 是否处于选择管理模式
 * @param isSelected 当前是否被选中
 * @param onToggleSelect 切换选中状态回调
 */
@Composable
fun SelectableNoteCard(
    item: FeedItem,
    liked: Boolean,
    onLikeToggle: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isSelectMode: Boolean = false,
    isSelected: Boolean = false,
    onToggleSelect: () -> Unit = {},
) {
    Box(modifier = modifier) {
        NoteCard(
            item = item,
            liked = liked,
            onLikeToggle = onLikeToggle,
            // 选择模式下点卡片切换选中，普通模式下跳转
            onClick = if (isSelectMode) onToggleSelect else onClick,
            modifier = Modifier.fillMaxWidth(),
        )
        if (isSelectMode) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 8.dp, end = 8.dp)
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) LimePrimary else Color.White.copy(alpha = 0.75f))
                    .then(
                        if (!isSelected) Modifier.border(1.5.dp, Color.White, CircleShape)
                        else Modifier
                    )
                    .clickable { onToggleSelect() },
                contentAlignment = Alignment.Center,
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
        }
    }
}
