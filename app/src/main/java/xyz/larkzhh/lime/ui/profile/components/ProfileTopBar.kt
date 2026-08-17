package xyz.larkzhh.lime.ui.profile.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import xyz.larkzhh.lime.data.network.model.UserData
import xyz.larkzhh.lime.ui.theme.LimeGray
import xyz.larkzhh.lime.ui.theme.LimePrimary
import xyz.larkzhh.lime.ui.theme.LimePrimaryPale
import xyz.larkzhh.lime.ui.theme.LimeWhite

@Composable
fun ProfileTopBar(
    user: UserData?,
    bgAlpha: Float,
    modifier: Modifier = Modifier,
    dominantColor: Color = Color.Black,
    miniAvatarAlpha: Float,
    miniAvatarOffsetDp: Dp,
    editButtonAlpha: Float,
    leadingIcon: ImageVector,
    onLeadingClick: () -> Unit,
    showTrailingActions: Boolean,
    onEditProfileClick: () -> Unit,
    onQrScanClick: () -> Unit,
    onSizeChanged: (IntSize) -> Unit = {},
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .onSizeChanged(onSizeChanged)
            .background(dominantColor.copy(alpha = bgAlpha))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 4.dp)
                .height(52.dp),
        ) {
            val leadingLabel =
                if (leadingIcon == Icons.AutoMirrored.Filled.ArrowBack) "返回" else "菜单"
            IconButton(
                onClick = onLeadingClick,
                modifier = Modifier.align(Alignment.CenterStart),
            ) {
                Icon(leadingIcon, contentDescription = leadingLabel, tint = LimeWhite)
            }

            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .alpha(miniAvatarAlpha)
                    .offset(y = miniAvatarOffsetDp),
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(LimePrimaryPale),
                    contentAlignment = Alignment.Center,
                ) {
                    if (user?.avatar != null) {
                        AsyncImage(
                            model = user.avatar,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                        )
                    } else {
                        Text(
                            text = user?.nickname?.take(1) ?: "",
                            style = MaterialTheme.typography.labelMedium,
                            color = LimePrimary,
                        )
                    }
                }
            }

            if (showTrailingActions) {
                Row(
                    modifier = Modifier.align(Alignment.CenterEnd),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        modifier = Modifier
                            .alpha(editButtonAlpha)
                            .background(
                                LimeGray.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(20.dp)
                            )
                            .clickable(
                                enabled = editButtonAlpha > 0.05f,
                                onClick = onEditProfileClick
                            )
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            tint = LimeWhite,
                            modifier = Modifier.size(14.dp),
                        )
                        Text(
                            text = "编辑主页",
                            style = MaterialTheme.typography.labelMedium,
                            color = LimePrimaryPale,
                        )
                    }
                    Spacer(Modifier.width(4.dp))
                    IconButton(onClick = onQrScanClick) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = "扫一扫", tint = LimeWhite)
                    }
                }
            }
        }
    }
}
