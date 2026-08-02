package xyz.larkzhh.lime.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RemoveRedEye
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import xyz.larkzhh.lime.data.network.model.FeedItem
import xyz.larkzhh.lime.ui.theme.LimeDark
import xyz.larkzhh.lime.ui.theme.LimeGray
import xyz.larkzhh.lime.ui.theme.LimeLightGray
import xyz.larkzhh.lime.ui.theme.LimePrimary
import xyz.larkzhh.lime.ui.theme.LimePrimaryPale

/**
 * 信息流笔记卡片组件
 *
 * @param item 信息流数据实体
 * @param liked 当前用户是否已点赞该笔记
 * @param onLikeToggle 点赞状态切换的回调
 * @param onClick 卡片的点击回调
 * @param modifier 外部传的 Modifier
 */
@Composable
fun NoteCard(
    item: FeedItem,
    liked: Boolean,
    onLikeToggle: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val imageRatio = remember(item.id) {
//        val idx = (item.id % 3).toInt().let { if (it < 0) it + 3 else it }
//        listOf(0.75f, 0.85f, 1.0f)[idx]
        val idx = (item.id % 4).toInt().let { if (it < 0) it + 4 else it }
        listOf(0.65f, 0.8f, 0.95f, 1.1f)[idx]
    }

    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column {
            // 封面图
            Box {
                if (item.coverImage != null) {
                    AsyncImage(
                        model = item.coverImage,
                        contentDescription = item.title,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(imageRatio)
                            .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    // 无封面占位
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(imageRatio)
                            .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                            .background(LimeLightGray),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = item.title?.take(4) ?: "图文",
                            style = MaterialTheme.typography.bodySmall,
                            color = LimeGray,
                        )
                    }
                }
                // 浏览数角标
                if (item.viewCount != null) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(start = 6.dp, bottom = 6.dp)
                                .background(LimeDark.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 3.dp, vertical = 0.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Default.RemoveRedEye,
                            contentDescription = "浏览人数",
                            tint = Color.White,
                            modifier = Modifier.size(10.dp),
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(
                            text = formatViewCount(item.viewCount),
                            fontSize = 10.sp,
                            color = Color.White,
                        )
                    }
                }
            }

            // 标题与作者
            Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
                if (!item.title.isNullOrBlank()) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    // 作者头像
                    if (item.author.avatar != null) {
                        AsyncImage(
                            model = item.author.avatar,
                            contentDescription = null,
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop,
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(LimePrimaryPale),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = item.author.nickname.take(1),// 提取作者昵称的第一个字
                                fontSize = 8.sp,
                                color = LimePrimary,
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = item.author.nickname,
                        style = MaterialTheme.typography.labelSmall,
                        color = LimeGray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    LikeButton(
                        liked = liked,
                        onToggle = onLikeToggle,
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = formatLikeCount(item.likeCount),
                        style = MaterialTheme.typography.labelSmall,
                        color = LimeGray,
                    )
                }
            }
        }
    }
}

fun formatLikeCount(count: Int): String = when {
    count >= 10000 -> "${count / 10000}w"// 就写在这吧，感觉这辈子都用不到
    else -> count.toString()
}

fun formatViewCount(count: Int): String = when {
    count >= 10000 -> "${count / 10000}w"// 同上
    else -> count.toString()
}
