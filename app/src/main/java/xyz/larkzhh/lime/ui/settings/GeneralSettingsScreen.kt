package xyz.larkzhh.lime.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import xyz.larkzhh.lime.ui.components.LimeAlertDialog
import xyz.larkzhh.lime.ui.components.LimeSwitch
import xyz.larkzhh.lime.ui.components.SheetGroup
import xyz.larkzhh.lime.ui.theme.LimeGray
import xyz.larkzhh.lime.ui.theme.LimeLightGray
import xyz.larkzhh.lime.util.showToast

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeneralSettingsScreen(
    onClose: () -> Unit,
    viewModel: GeneralSettingsViewModel = hiltViewModel(),
) {
    val notifyEnabled by viewModel.notifyEnabled.collectAsState()
    val cacheLabel by viewModel.cacheLabel.collectAsState()
    var showClearDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // 刷新缓存大小
    LaunchedEffect(Unit) { viewModel.refreshCacheSize() }

    BackHandler { onClose() }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "通用",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = LimeLightGray,
                    scrolledContainerColor = Color.Unspecified,
                    navigationIconContentColor = Color.Unspecified,
                    titleContentColor = Color.Unspecified,
                    actionIconContentColor = Color.Unspecified,
                ),
            )
        },
        containerColor = LimeLightGray,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(top = 12.dp),
        ) {
            SectionLabel("通知")

            SheetGroup(cardColor = Color.White) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.NotificationsActive,
                        contentDescription = null,
                        tint = Color(0xFF1C1C1E),
                        modifier = Modifier.width(24.dp),
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "通知提醒",
                        fontSize = 15.sp,
                        color = Color(0xFF1C1C1E),
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    LimeSwitch(
                        checked = notifyEnabled,
                        onCheckedChange = viewModel::setNotifyEnabled,
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            SectionLabel("存储")

            SheetGroup(cardColor = Color.White) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() },
                        ) { showClearDialog = true }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.DeleteOutline,
                        contentDescription = null,
                        tint = Color(0xFF1C1C1E),
                        modifier = Modifier.width(24.dp),
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "清空缓存",
                        fontSize = 15.sp,
                        color = Color(0xFF1C1C1E),
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = cacheLabel,
                        fontSize = 14.sp,
                        color = LimeGray,
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }

    // 清空缓存二次确认
    if (showClearDialog) {
        LimeAlertDialog(
            title = "确清除应用缓存吗？",
            firstButtonText = "取消",
            secondButtonText = "清空",
            secondButtonColor = Color(0xFFFF3B30),
            onFirstButtonClick = { showClearDialog = false },
            onSecondButtonClick = {
                showClearDialog = false
                viewModel.clearCache { label ->
                    "缓存已清理（剩余 $label）".showToast(context)
                }
            },
            onDismissRequest = { showClearDialog = false },
        )
    }
}

/// 小节标题
@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        fontSize = 13.sp,
        color = LimeGray,
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
    )
}
