package xyz.larkzhh.lime.ui.profile.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import xyz.larkzhh.lime.R
import xyz.larkzhh.lime.ui.theme.LimeGray
import xyz.larkzhh.lime.ui.theme.LimeTheme
import xyz.larkzhh.lime.ui.theme.LimeWhite

/**
 * 个人主页统计说明弹窗
 */
@Composable
fun LikeFavStatsDialog(
    noteCount: Int,
    likeCount: Int,
    favCount: Int,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        LikeFavStatsDialogContent(
            noteCount = noteCount,
            likeCount = likeCount,
            favCount = favCount,
            onDismiss = onDismiss,
        )
    }
}

@Composable
private fun LikeFavStatsDialogContent(
    noteCount: Int,
    likeCount: Int,
    favCount: Int,
    onDismiss: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.width(260.dp),// 窄而竖，高比宽长
    ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp, vertical = 28.dp),
            ) {
                Text(
                    text = stringResource(R.string.profile_likes_favs),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(Modifier.height(20.dp))
                HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)

                Spacer(Modifier.height(14.dp))
                StatsRow(
                    icon = Icons.AutoMirrored.Outlined.Article,
                    label = stringResource(R.string.profile_stat_notes),
                    value = noteCount,
                )
                StatsRow(
                    icon = Icons.Outlined.FavoriteBorder,
                    label = stringResource(R.string.profile_stat_likes),
                    value = likeCount,
                )
                StatsRow(
                    icon = Icons.Outlined.StarBorder,
                    label = stringResource(R.string.profile_stat_favs),
                    value = favCount,
                )

                Spacer(Modifier.height(26.dp))

                // 我知道了
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(MaterialTheme.colorScheme.primary)
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() },
                            onClick = onDismiss,
                        )
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.profile_got_it),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = LimeWhite,
                    )
                }
            }
        }
}

@Composable
private fun StatsRow(icon: ImageVector, label: String, value: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = LimeGray,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = LimeGray,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun LikeFavStatsDialogPreview() {
    LimeTheme {
        LikeFavStatsDialogContent(
            noteCount = 12,
            likeCount = 128,
            favCount = 36,
            onDismiss = {},
        )
    }
}
