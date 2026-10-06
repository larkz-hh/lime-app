package xyz.larkzhh.lime.ui.profile.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.annotation.StringRes
import xyz.larkzhh.lime.feature.profile.R
import xyz.larkzhh.lime.core.designsystem.components.SheetGroup
import xyz.larkzhh.lime.ui.theme.LimeLightGray

/**
 * 我的页面左侧抽屉内容
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileDrawerContent(
    onTranslateClick: () -> Unit,
    onDraftsClick: () -> Unit,
    onAccountPrivacyClick: () -> Unit,
    onGeneralClick: () -> Unit,
    onAboutClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ModalDrawerSheet(
        modifier = modifier.width(280.dp),
        drawerContainerColor = MaterialTheme.colorScheme.background,
        drawerShape = RectangleShape,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 14.dp, vertical = 25.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SheetGroup(cardColor = MaterialTheme.colorScheme.surface) {
                DrawerRow(
                    icon = Icons.Outlined.Person,
                    labelRes = R.string.drawer_account_privacy,
                    onClick = onAccountPrivacyClick,
                )
            }
            SheetGroup(cardColor = MaterialTheme.colorScheme.surface) {
                DrawerRow(
                    icon = Icons.Outlined.Description,
                    labelRes = R.string.drawer_drafts,
                    onClick = onDraftsClick,
                )
            }
            SheetGroup(cardColor = MaterialTheme.colorScheme.surface) {
                DrawerRow(
                    icon = Icons.Outlined.Settings,
                    labelRes = R.string.drawer_general,
                    onClick = onGeneralClick,
                )
            }
            SheetGroup(cardColor = MaterialTheme.colorScheme.surface) {
                DrawerRow(
                    icon = Icons.Outlined.Translate,
                    labelRes = R.string.drawer_offline,
                    onClick = onTranslateClick,
                )
            }
            SheetGroup(cardColor = MaterialTheme.colorScheme.surface) {
                DrawerRow(
                    icon = Icons.Outlined.Info,
                    labelRes = R.string.drawer_about,
                    onClick = onAboutClick,
                )
            }
        }
    }
}

/// 抽屉单行选项
@Composable
private fun DrawerRow(
    icon: ImageVector,
    @StringRes labelRes: Int,
    onClick: () -> Unit,
) {
    val label = stringResource(labelRes)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClick,
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(24.dp),
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 17.sp,
            modifier = Modifier.weight(1f),
        )
    }
}
