package xyz.larkzhh.lime.ui.profile.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import xyz.larkzhh.lime.R
import xyz.larkzhh.lime.data.network.model.UserData
import xyz.larkzhh.lime.domain.model.FollowActionState
import xyz.larkzhh.lime.ui.components.labelRes
import xyz.larkzhh.lime.ui.profile.viewmodel.ProfileUiState
import xyz.larkzhh.lime.ui.theme.LimeGray
import xyz.larkzhh.lime.ui.theme.LimePrimaryPale
import xyz.larkzhh.lime.ui.theme.LimeWhite
import java.time.LocalDate

@Composable
fun ProfileHeader(
    uiState: ProfileUiState,
    isSelf: Boolean,
    onEditAvatar: () -> Unit,
    modifier: Modifier = Modifier,
    onAvatarClick: (() -> Unit)? = null, // 他人头像点击：进入全屏预览
    gradientEndColor: Color = Color.Black.copy(alpha = 0.9f),
    onBrowseHistory: () -> Unit = {},
    onGroupChat: () -> Unit = {},
    onFollowClick: () -> Unit = {},
    onMessageClick: () -> Unit = {},
    onFollowingClick: () -> Unit = {},
    onFollowersClick: () -> Unit = {},
    onLikeFavClick: () -> Unit = {},
    followState: FollowActionState = FollowActionState.Follow,
) {
    val user = (uiState as? ProfileUiState.Success)?.user
    val backgroundUrl = user?.backgroundImage

    Box(
        modifier = modifier
            .fillMaxWidth()
    ) {
        // 背景图
        if (backgroundUrl != null) {
            AsyncImage(
                model = backgroundUrl,
                contentDescription = null,
                modifier = Modifier.matchParentSize(),
                contentScale = ContentScale.Crop,
                colorFilter = ColorFilter.colorMatrix(
                    ColorMatrix().apply { setToScale(0.52f, 0.52f, 0.52f, 1f) }
                ),// 亮度变暗
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    //.graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            ) {
//                AsyncImage(
//                    model = backgroundUrl,
//                    contentDescription = null,
//                    modifier = Modifier
//                        .matchParentSize()
//                        .blur(20.dp),
//                    contentScale = ContentScale.Crop,
//                )
                BlurMask(gradientEndColor)
            }
        } else {
            // 无背景图时本地默认背景
            Image(
                painter = painterResource(R.drawable.bg),
                contentDescription = null,
                modifier = Modifier.matchParentSize(),
                contentScale = ContentScale.Crop,
                colorFilter = ColorFilter.colorMatrix(
                    ColorMatrix().apply { setToScale(0.52f, 0.52f, 0.52f, 1f) }
                ),
            )
            BlurMask(gradientEndColor)
        }

        Column(
            modifier = Modifier
                .statusBarsPadding()
                .padding(top = 56.dp, start = 16.dp, end = 16.dp, bottom = 28.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            /// 头像与昵称
            Row(verticalAlignment = Alignment.CenterVertically) {
                AvatarSection(
                    user = user,
                    editable = isSelf,
                    onClick = onEditAvatar,
                    onAvatarClick = onAvatarClick,
                )
                Spacer(Modifier.width(16.dp))
                UserInfoSection(user = user)
            }

            Spacer(Modifier.height(16.dp))

            // 关注/粉丝/获赞与收藏
            Row(horizontalArrangement = Arrangement.Start) {
                StatItem(
                    count = (user?.followingCount ?: 0).toString(),
                    label = stringResource(R.string.profile_following),
                    onClick = onFollowingClick,
                )
                Spacer(Modifier.width(28.dp))
                StatItem(
                    count = (user?.followerCount ?: 0).toString(),
                    label = stringResource(R.string.profile_followers),
                    onClick = onFollowersClick,
                )
                Spacer(Modifier.width(28.dp))
                StatItem(
                    count = ((user?.totalLikeCount ?: 0) + (user?.totalFavCount ?: 0)).toString(),
                    label = stringResource(R.string.profile_likes_favs),
                    onClick = onLikeFavClick,
                )
            }

            Spacer(Modifier.height(14.dp))

            // 个人简介
            Text(
                text = user?.bio?.takeIf { it.isNotBlank() } ?: stringResource(R.string.profile_bio_empty),
                style = MaterialTheme.typography.bodyMedium,
                //color = if (user?.bio?.isNotBlank() == true) LimeDark else LimeGray,
                color = LimeWhite,
                maxLines = 3,
            )
            Spacer(Modifier.height(16.dp))

            // 性别、年龄、地区标签
            val age = user?.birthday?.let { calculateAge(it) }
            val genderIcon: Pair<String, Color>? = when (user?.gender) {
                1 -> "♂" to Color(0xFF5B9BD5)
                2 -> "♀" to Color(0xFFE91E8C)
                else -> null
            }
            val ageText = age?.let { stringResource(R.string.profile_age_years, it) }
            val showAgeGenderChip = ageText != null || genderIcon != null
            if (showAgeGenderChip || user?.region?.isNotBlank() == true) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // 性别、年龄一个标签
                    if (showAgeGenderChip) InfoChip(text = ageText, genderIcon = genderIcon)
                    // 地区一个标签
                    if (user?.region?.isNotBlank() == true) InfoChip(text = user.region)
                }
                Spacer(Modifier.height(12.dp))
            }

            Spacer(Modifier.height(16.dp))

            if (isSelf) {
                // 浏览记录/群聊
                Row(modifier = Modifier.fillMaxWidth()) {
                    QuickCard(
                        icon = Icons.Default.History,
                        label = stringResource(R.string.profile_browse_history),
                        subtitle = stringResource(R.string.profile_browse_history_desc),
                        modifier = Modifier.weight(1f),
                        onClick = onBrowseHistory,
                    )
                    Spacer(Modifier.width(12.dp))
                    QuickCard(
                        icon = Icons.Default.Groups,
                        label = stringResource(R.string.profile_group_chat),
                        subtitle = stringResource(R.string.profile_group_chat_desc),
                        modifier = Modifier.weight(1f),
                        onClick = onGroupChat,
                    )
                }
            } else {
                // 关注/发私信
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    ActionButton(
                        text = stringResource(followState.labelRes),
                        filled = followState == FollowActionState.Follow,
                        modifier = Modifier.weight(1f),
                        onClick = onFollowClick,
                    )
                    ActionButton(
                        text = stringResource(R.string.profile_send_message),
                        filled = false,
                        modifier = Modifier.weight(1f),
                        onClick = onMessageClick,
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

/// 头像区域
@Composable
private fun AvatarSection(
    user: UserData?,
    editable: Boolean,
    onClick: () -> Unit,
    onAvatarClick: (() -> Unit)? = null,
) {
    // 自己：点击进入裁剪/编辑（原有行为）；他人且有头像：点击进入全屏预览
    val clickModifier = when {
        editable -> Modifier.clickable(onClick = onClick)
        onAvatarClick != null && user?.avatar != null -> Modifier.clickable(onClick = onAvatarClick)
        else -> Modifier
    }
    Box(
        modifier = Modifier
            .size(76.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .then(clickModifier),
        contentAlignment = Alignment.Center,
    ) {
        if (user?.avatar != null) {
            AsyncImage(
                model = user.avatar,
                contentDescription = stringResource(R.string.profile_avatar),
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else if (editable) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = null,
                    tint = LimeGray,
                    modifier = Modifier.size(26.dp),
                )
                Text(
                    text = stringResource(R.string.profile_upload_avatar),
                    style = MaterialTheme.typography.labelSmall,
                    color = LimeGray,
                )
            }
        } else {
            // 他人无头像
            Text(
                text = user?.nickname?.take(1) ?: "?",
                style = MaterialTheme.typography.titleLarge,
                color = LimeGray,
            )
        }
    }
}

/// 昵称与号码
@Composable
private fun UserInfoSection(user: UserData?) {
    Column {
        Text(
            text = user?.nickname ?: stringResource(R.string.profile_nickname_unset),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = LimeWhite,
        )
        // 号码
        if (user != null) {
            Spacer(Modifier.height(2.dp))
            Text(
                text = "ID: ${user.handle}",
                style = MaterialTheme.typography.bodySmall,
                color = LimeGray,
            )
        }
    }
}

/// 数字标签
@Composable
private fun StatItem(count: String, label: String, onClick: (() -> Unit)? = null) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier,
    ) {
        Text(
            text = count,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = LimeWhite,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = LimePrimaryPale,
        )
    }
}

/// 年龄、地区标签
@Composable
private fun InfoChip(text: String? = null, genderIcon: Pair<String, Color>? = null) {
    Row(
        modifier = Modifier
            .background(Color.Gray.copy(alpha = 0.4f), shape = RoundedCornerShape(20.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        if (genderIcon != null) {
            Text(
                text = genderIcon.first,
                style = MaterialTheme.typography.labelSmall,
                color = genderIcon.second,
            )
        }
        if (text != null) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
            )
        }
    }
}

/// 根据生日字符串计算周岁
private fun calculateAge(birthday: String): Int? = runCatching {
//    val parts = birthday.split("-")
    val birth = LocalDate.parse(birthday)
    val today = LocalDate.now()
//    val birth = LocalDate.of(parts[0].toInt(), parts[1].toInt(), parts[2].toInt())
//    var age = today.year - birth.year
//    if (today.monthValue < birth.monthValue ||
//        (today.monthValue == birth.monthValue && today.dayOfMonth < birth.dayOfMonth)) age--
    val age = java.time.temporal.ChronoUnit.YEARS.between(birth, today).toInt()
    age.takeIf { it >= 0 }
}.getOrNull()

/// 快捷入口卡片
@Composable
private fun QuickCard(
    icon: ImageVector,
    label: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Gray.copy(alpha = 0.4f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = LimeWhite,
                modifier = Modifier.size(22.dp),
            )
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = LimeWhite,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = LimeGray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/// 关注/发私信按钮
@Composable
private fun ActionButton(
    text: String,
    filled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .then(
                if (filled) Modifier.background(MaterialTheme.colorScheme.primary)
                else Modifier.background(Color.White.copy(alpha = 0.3f))
            )
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = LimeWhite,
        )
    }
}

/// 蒙版
@Composable
private fun BoxScope.BlurMask(gradientEndColor: Color) {
    Box(
        modifier = Modifier
            .matchParentSize()
            //.graphicsLayer { blendMode = BlendMode.DstIn }
            .background(
                brush = Brush.verticalGradient(
                    0f to Color.Transparent,
                    0.5f to Color.Black.copy(0.3f),
                    0.7f to gradientEndColor.copy(0.9f),
                )
            )
    )
}