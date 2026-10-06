package xyz.larkzhh.lime.ui.detail.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import xyz.larkzhh.lime.core.designsystem.R as DesignSystemR
import xyz.larkzhh.lime.data.network.model.NoteDetailData
import xyz.larkzhh.lime.domain.model.FollowActionState
import xyz.larkzhh.lime.ui.components.FollowButton
import xyz.larkzhh.lime.ui.theme.LimeLightGray

/// 顶部栏
@Composable
fun AuthorBar(
    note: NoteDetailData,
    onBack: () -> Unit,
    onAuthorClick: () -> Unit,
    followState: FollowActionState? = null,
    onFollowClick: () -> Unit = {},
    onShareClick: () -> Unit = {},
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 4.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(DesignSystemR.string.back),
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }

        // 头像
        Row(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(20.dp))
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = onAuthorClick,
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AsyncImage(
                model = note.author.avatar,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            )

            Spacer(modifier = Modifier.width(10.dp))

            // 昵称
            Text(
                text = note.author.nickname,
                fontWeight = FontWeight.Medium,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // 关注按钮
        if (followState != null) {
            FollowButton(
                state = followState,
                onClick = onFollowClick,
            )
        }

        // 分享按钮
        IconButton(onClick = onShareClick) {
            Icon(
                painter = painterResource(DesignSystemR.drawable.ic_share),
                contentDescription = stringResource(DesignSystemR.string.video_share),
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
