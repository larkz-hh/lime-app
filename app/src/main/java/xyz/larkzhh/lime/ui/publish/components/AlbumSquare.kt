package xyz.larkzhh.lime.ui.publish.components

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import xyz.larkzhh.lime.R
import xyz.larkzhh.lime.ui.theme.LimePrimary

/// 相册入口
@Composable
fun AlbumSquare(
    albumUri: Uri?,
    selected: Boolean,
    onPick: () -> Unit,
    onUseAlbum: () -> Unit,
    onClear: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .clickable {
                when {
                    albumUri == null -> onPick()// 打开相册
                    !selected -> onUseAlbum()// 切回相册图
                    else -> onPick()// 重新选一张
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        if (albumUri != null) {
            AsyncImage(
                model = albumUri,
                contentDescription = stringResource(R.string.album_cover),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            if (selected) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(LimePrimary.copy(alpha = 0.18f))
                )
            }
            // 清掉相册图回截帧
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(2.dp)
                    .size(16.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = 0.55f))
                    .clickable { onClear() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = stringResource(R.string.remove_album_cover),
                    tint = Color.White,
                    modifier = Modifier.size(11.dp),
                )
            }
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Filled.Add,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .size(18.dp),
                )
                Text(
                    stringResource(R.string.chat_album),
                    fontSize = 10.sp,
                    color = Color.White.copy(alpha = 0.6f),
                )
            }
        }
    }
}
