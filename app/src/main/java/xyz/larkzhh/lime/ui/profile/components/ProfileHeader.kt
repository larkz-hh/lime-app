package xyz.larkzhh.lime.ui.profile.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import xyz.larkzhh.lime.data.network.model.UserData
import xyz.larkzhh.lime.ui.profile.viewmodel.ProfileUiState
import xyz.larkzhh.lime.ui.theme.LimeDark
import xyz.larkzhh.lime.ui.theme.LimeGray
import xyz.larkzhh.lime.ui.theme.LimePrimary
import xyz.larkzhh.lime.ui.theme.LimePrimaryLight
import xyz.larkzhh.lime.ui.theme.LimePrimaryPale
import java.time.LocalDate


@Composable
fun ProfileHeader(
    uiState: ProfileUiState,
    onEditProfile: () -> Unit,
    onEditAvatar: () -> Unit,
) {
    val user = (uiState as? ProfileUiState.Success)?.user
    val backgroundUrl = user?.backgroundImage

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (backgroundUrl == null)
                    Modifier.background(
                        brush = Brush.verticalGradient(
                            colors = listOf(LimePrimaryLight, LimePrimaryPale)
                        )
                    )
                else Modifier
            )
    ) {
        if (backgroundUrl != null) {
            AsyncImage(
                model = backgroundUrl,
                contentDescription = null,
                modifier = Modifier
                    .matchParentSize()
                    .blur(1.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded),
                contentScale = ContentScale.Crop,
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.25f),
                                Color.Black.copy(alpha = 0.50f),
                            )
                        )
                    )
            )
        }
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {

            // 顶部工具栏 — statusBarsPadding 让内容在状态栏图标下方
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { /* TODO: 打开抽屉 */ }) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "菜单",
                        tint = LimeDark,
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(
                        onClick = onEditProfile,
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, LimePrimary),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                        modifier = Modifier.height(34.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = LimePrimary),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("编辑主页", style = MaterialTheme.typography.labelMedium)
                    }
                    Spacer(Modifier.width(4.dp))
                    IconButton(onClick = { /* TODO: 扫一扫 */ }) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = "扫一扫",
                            tint = LimeDark,
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            /// 头像与昵称
            Row(verticalAlignment = Alignment.CenterVertically) {
                AvatarSection(avatarUrl = user?.avatar, onClick = onEditAvatar)
                Spacer(Modifier.width(16.dp))
                UserInfoSection(user = user)
            }

            Spacer(Modifier.height(16.dp))

            // 关注/粉丝/获赞与收藏
            Row(horizontalArrangement = Arrangement.Start) {
                StatItem(count = "0", label = "关注")
                Spacer(Modifier.width(28.dp))
                StatItem(count = "0", label = "粉丝")
                Spacer(Modifier.width(28.dp))
                StatItem(count = "0", label = "获赞与收藏")
            }

            Spacer(Modifier.height(14.dp))

            // 个人简介
            Text(
                text = user?.bio?.takeIf { it.isNotBlank() } ?: "这个人是懒猪猪，还没有填写简介~",
                style = MaterialTheme.typography.bodyMedium,
                //color = if (user?.bio?.isNotBlank() == true) LimeDark else LimeGray,
                color = LimePrimaryPale,
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
            val ageText = age?.let { "${it}岁" }
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

            // 浏览记录/群聊
            Row(modifier = Modifier.fillMaxWidth()) {
                QuickCard(
                    icon = Icons.Default.History,
                    label = "浏览记录",
                    subtitle = "看过的笔记",
                    modifier = Modifier.weight(1f),
                    onClick = { /* TODO */ },
                )
                Spacer(Modifier.width(12.dp))
                QuickCard(
                    icon = Icons.Default.Groups,
                    label = "群聊",
                    subtitle = "查看详情",
                    modifier = Modifier.weight(1f),
                    onClick = { /* TODO */ },
                )
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

/// 头像区域
@Composable
private fun AvatarSection(avatarUrl: String?, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(76.dp)
            .clip(CircleShape)
            .background(Color(0xFFD4EAE0))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (avatarUrl != null) {
            AsyncImage(
                model = avatarUrl,
                contentDescription = "头像",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
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
                    text = "上传头像",
                    style = MaterialTheme.typography.labelSmall,
                    color = LimeGray,
                )
            }
        }
    }
}

/// 昵称与号码
@Composable
private fun UserInfoSection(user: UserData?) {
    Column {
        Text(
            text = user?.nickname ?: "未设置昵称",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = LimePrimaryPale,
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
private fun StatItem(count: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = count,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = LimePrimaryPale,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = LimeGray,
        )
    }
}

/// 年龄、地区标签
@Composable
private fun InfoChip(text: String? = null, genderIcon: Pair<String, Color>? = null) {
    Row(
        modifier = Modifier
            .background(Color.Gray.copy(alpha = 0.5f), shape = RoundedCornerShape(20.dp))
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
        colors = CardDefaults.cardColors(containerColor = Color.Gray.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = LimePrimaryPale,
                modifier = Modifier.size(22.dp),
            )
            Spacer(Modifier.width(8.dp))
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = LimePrimaryPale,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = LimeGray,
                )
            }
        }
    }
}