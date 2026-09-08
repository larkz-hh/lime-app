package xyz.larkzhh.lime.ui.components

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import xyz.larkzhh.lime.R
import xyz.larkzhh.lime.ui.theme.LimeGray

/// 空内容加载失败状态
@Composable
fun ErrorState(
    message: String?,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = message ?: stringResource(R.string.error_load_failed),
            color = LimeGray,
            fontSize = 14.sp,
        )
        Spacer(modifier = Modifier.height(12.dp))
        RetryButton(onRetry = onRetry)
    }
}

/// 通用重试按钮
@Composable
fun RetryButton(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onRetry)
            .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Outlined.Refresh,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(16.dp),
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = stringResource(R.string.retry), color = MaterialTheme.colorScheme.primary, fontSize = 13.sp)
    }
}

/// 表底部重试
@Composable
fun LoadMoreErrorItem(
    message: String?,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Text(
            text = message ?: stringResource(R.string.error_load_failed),
            color = LimeGray,
            fontSize = 12.sp,
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = stringResource(R.string.retry),
            color = MaterialTheme.colorScheme.primary,
            fontSize = 12.sp,
            modifier = Modifier.clickable(onClick = onRetry),
        )
    }
}
