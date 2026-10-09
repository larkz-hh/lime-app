package xyz.larkzhh.lime.ui.components.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import xyz.larkzhh.lime.core.designsystem.R as DesignSystemR
import xyz.larkzhh.lime.core.ui.R
import xyz.larkzhh.lime.ui.theme.LimeGray
import xyz.larkzhh.lime.ui.theme.LimeLightGray

/**
 * AI 聊天底部栏
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatAddSheet(
    onDismiss: () -> Unit,
    onCamera: () -> Unit,
    onAlbum: () -> Unit,
    onNote: () -> Unit,
    webSearch: Boolean,
    onWebSearchChange: (Boolean) -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 20.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                AddSquare(
                    icon = Icons.Outlined.PhotoCamera,
                    label = stringResource(R.string.chat_take_photo),
                    onClick = onCamera,
                    modifier = Modifier.weight(1f),
                )
                AddSquare(
                    icon = Icons.Outlined.PhotoLibrary,
                    label = stringResource(DesignSystemR.string.chat_album),
                    onClick = onAlbum,
                    modifier = Modifier.weight(1f),
                )
                AddSquare(
                    icon = Icons.Outlined.Edit,
                    label = stringResource(R.string.chat_attach_note),
                    onClick = onNote,
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(Modifier.height(16.dp))

            WebSearchRow(webSearch = webSearch, onChange = onWebSearchChange)
        }
    }
}

@Composable
private fun AddSquare(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                icon,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(30.dp),
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 13.sp,
        )
    }
}

@Composable
private fun WebSearchRow(
    webSearch: Boolean,
    onChange: (Boolean) -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable { menuExpanded = true }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Outlined.Public,
            contentDescription = stringResource(R.string.chat_web_search),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = stringResource(R.string.chat_web_search),
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 14.sp,
        )
        Spacer(Modifier.weight(1f))
        Text(
            text = if (webSearch) stringResource(R.string.chat_web_search_auto)
            else stringResource(R.string.chat_web_search_off),
            color = LimeGray,
            fontSize = 14.sp,
        )
        Icon(
            Icons.Filled.ArrowDropDown,
            contentDescription = null,
            tint = LimeGray,
            modifier = Modifier.size(20.dp),
        )

        Box(modifier = Modifier.align(Alignment.CenterVertically)) {
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false },
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                shape = RoundedCornerShape(12.dp),
            ) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.chat_web_search_auto)) },
                    onClick = {
                        onChange(true)
                        menuExpanded = false
                    },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.chat_web_search_off)) },
                    onClick = {
                        onChange(false)
                        menuExpanded = false
                    },
                )
            }
        }
    }
}
