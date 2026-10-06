package xyz.larkzhh.lime.ui.detail.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import xyz.larkzhh.lime.core.designsystem.R as DesignSystemR
import xyz.larkzhh.lime.feature.detail.R
import xyz.larkzhh.lime.data.network.model.NoteDetailData
import xyz.larkzhh.lime.ui.components.FavoriteButton
import xyz.larkzhh.lime.ui.components.LikeButton
import xyz.larkzhh.lime.ui.theme.LimeGray
import xyz.larkzhh.lime.ui.theme.LimeLightGray

@Composable
fun NoteBottomBar(
    note: NoteDetailData,
    onToggleLike: () -> Unit,
    onToggleFavorite: () -> Unit,
    onCommentClick: () -> Unit,
    isAuthor: Boolean = false,
    onManageClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.background,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    inputBackground: Color = MaterialTheme.colorScheme.surfaceVariant,
    elevated: Boolean = true,
    compact: Boolean = false,
) {
    val rowVertical = if (compact) 6.dp else 10.dp
    val inputVertical = if (compact) 6.dp else 10.dp
    val inputClick: () -> Unit =
        if (isAuthor) (onManageClick ?: onCommentClick) else onCommentClick
    Surface(
        modifier = modifier,
        shadowElevation = if (elevated) 8.dp else 0.dp,
        color = containerColor,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 12.dp, vertical = rowVertical),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            // 评论输入框或编辑设置
            if (isAuthor) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() },
                            onClick = inputClick,
                        )
                        .padding(horizontal = 4.dp, vertical = rowVertical),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Text(
                        text = stringResource(R.string.note_manage_entry),
                        color = contentColor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(50))
                        .background(inputBackground)
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() },
                            onClick = inputClick,
                        )
                        .padding(horizontal = 14.dp, vertical = inputVertical),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Text(text = stringResource(DesignSystemR.string.video_comment_hint), color = LimeGray, fontSize = 13.sp)
                }
            }

            // 点赞
            Box(
                modifier = Modifier.width(63.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    LikeButton(
                        liked = note.liked,
                        onToggle = onToggleLike,
                        iconSize = 24.dp,
                        animationSize = 32.dp,
                        inactiveColor = contentColor,
                    )
                    Text(
                        text = if (note.likeCount > 0) note.likeCount.toString() else stringResource(DesignSystemR.string.like),
                        fontSize = 12.sp,
                        color = contentColor,
                        maxLines = 1,
                    )
                }
            }

            // 收藏
            Box(
                modifier = Modifier.width(63.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    FavoriteButton(
                        favorited = note.favorited,
                        onToggle = onToggleFavorite,
                        modifier = Modifier.size(32.dp),
                        iconSize = 24.dp,
                        inactiveColor = contentColor,
                    )
                    Text(
                        text = if (note.favCount > 0) note.favCount.toString() else stringResource(DesignSystemR.string.favorite),
                        fontSize = 12.sp,
                        color = contentColor,
                        maxLines = 1,
                    )
                }
            }

            // 评论
            Box(
                modifier = Modifier.width(63.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    IconButton(onClick = onCommentClick, modifier = Modifier.size(32.dp)) {
                        Icon(
                            painter = painterResource(DesignSystemR.drawable.ic_chat),
                            contentDescription = stringResource(R.string.comment),
                            tint = contentColor,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                    Text(
                        text = if (note.commentCount > 0) note.commentCount.toString() else stringResource(R.string.comment),
                        fontSize = 12.sp,
                        color = contentColor,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}
