package xyz.larkzhh.lime.ui.detail.components

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import xyz.larkzhh.lime.data.network.model.NoteDetailData
import xyz.larkzhh.lime.ui.theme.LimeDark
import xyz.larkzhh.lime.ui.theme.LimeLightGray
import xyz.larkzhh.lime.ui.theme.LimePrimary

/// 顶部栏
@Composable
fun AuthorBar(
    note: NoteDetailData,
    onBack: () -> Unit,
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
                contentDescription = "返回",
                tint = LimeDark,
            )
        }

        // 头像
        AsyncImage(
            model = note.author.avatar,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(LimeLightGray),
        )

        Spacer(modifier = Modifier.width(10.dp))

        // 昵称
        Text(
            text = note.author.nickname,
            fontWeight = FontWeight.Medium,
            fontSize = 15.sp,
            color = LimeDark,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )

        Spacer(modifier = Modifier.width(8.dp))

        // 关注按钮
        Surface(
            shape = RoundedCornerShape(50),
            color = Color.Transparent,
            onClick = {},
        ) {
            Text(
                text = "关注",
                color = LimePrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            )
        }

        // 分享按钮
        IconButton(onClick = {}) {
            Icon(
                imageVector = Icons.Default.Share,
                contentDescription = "分享",
                tint = LimeDark,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
