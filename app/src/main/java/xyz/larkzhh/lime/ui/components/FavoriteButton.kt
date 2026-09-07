package xyz.larkzhh.lime.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import xyz.larkzhh.lime.R
import xyz.larkzhh.lime.ui.auth.LoginGate
import xyz.larkzhh.lime.ui.theme.LimeGray

/**
 * 收藏按钮
 *
 * @param favorited 是否已收藏
 * @param onToggle 点击回调
 * @param modifier 外部传入的 Modifier
 * @param iconSize 星形图标大小
 * @param activeColor 已收藏时的图标颜色
 * @param inactiveColor 未收藏时的图标颜色
 */
@Composable
fun FavoriteButton(
    favorited: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    iconSize: Dp = 24.dp,
    activeColor: Color = Color(0xFFFFD700),
    inactiveColor: Color = LimeGray,
) {
    Box(
        modifier = modifier.clickable(
            indication = null,
            interactionSource = remember { MutableInteractionSource() },
            onClick = {
                // 收藏登录拦截
                if (!LoginGate.onRequireLogin(null)) onToggle()
            },
        ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(if (favorited) R.drawable.ic_favorite_filled else R.drawable.ic_favorite),
            contentDescription = if (favorited) stringResource(R.string.unfavorite) else stringResource(R.string.favorite),
            tint = if (favorited) activeColor else inactiveColor,
            modifier = Modifier.size(iconSize),
        )
    }
}
