package xyz.larkzhh.lime.ui.home

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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
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
import xyz.larkzhh.lime.ui.theme.LimeGray
import xyz.larkzhh.lime.ui.theme.LimeLightGray
import xyz.larkzhh.lime.ui.theme.LimePrimary
import xyz.larkzhh.lime.ui.theme.LimePrimaryPale

@Composable
fun NoteCard(
    item: FeedItem,
    liked: Boolean,
    onLikeToggle: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val imageRatio = remember(item.id) {
        val idx = (item.id % 3).toInt().let { if (it < 0) it + 3 else it }
        listOf(0.75f, 0.85f, 1.0f)[idx]
    }
    val likeComposition by rememberLottieComposition(
        LottieCompositionSpec.Asset("lottie/like.lottie")
    )
    var isAnimating by remember { mutableStateOf(false) }
    val animationProgress by animateLottieCompositionAsState(
        composition = likeComposition,
        isPlaying = isAnimating,
        iterations = 1,
        restartOnPlay = true,
    )
    LaunchedEffect(animationProgress) {
        if (animationProgress >= 1f && isAnimating) isAnimating = false
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
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clickable{
                                if (!liked) isAnimating = true// 播放动画
                                onLikeToggle()
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (liked && isAnimating) {
                            LottieAnimation(
                                composition = likeComposition,
                                progress = { animationProgress },
                                modifier = Modifier.size(32.dp),
                            )
                        } else {
                            Icon(
                                imageVector = if (liked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                contentDescription = if (liked) "取消点赞" else "点赞",
                                modifier = Modifier.size(14.dp),
                                tint = if (liked) Color.Red else LimeGray,
                            )
                        }
                    }
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

private fun formatLikeCount(count: Int): String = when {
    count >= 10000 -> "${count / 10000}w"// 就写在这吧，感觉这辈子都用不到
    else -> count.toString()
}
