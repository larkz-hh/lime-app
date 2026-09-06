package xyz.larkzhh.lime.ui.translate

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import xyz.larkzhh.lime.R
import xyz.larkzhh.lime.data.local.TranslateMode
import xyz.larkzhh.lime.ui.components.LimeAlertDialog
import xyz.larkzhh.lime.ui.components.SheetGroup
import xyz.larkzhh.lime.ui.components.SheetRowDivider
import xyz.larkzhh.lime.ui.theme.LimeGray
import xyz.larkzhh.lime.util.TranslateModelInfo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranslatePackScreen(
    onBack: () -> Unit,
    viewModel: TranslatePackViewModel = hiltViewModel(),
) {
    // 进入页面时刷新语言包状态
    LaunchedEffect(Unit) { viewModel.refresh() }

    val uiState by viewModel.uiState.collectAsState()
    val mode by viewModel.mode.collectAsState()
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val packSizeLabel = TranslateModelInfo.downloadSizeLabel()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.translate_settings_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = Color.Unspecified,
                    navigationIconContentColor = Color.Unspecified,
                    titleContentColor = Color.Unspecified,
                    actionIconContentColor = Color.Unspecified
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .padding(top = 12.dp),
        ) {
            // 离线包分组
            Text(
                text = stringResource(R.string.translate_offline_pack),
                fontSize = 13.sp,
                color = LimeGray,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surface),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Translate,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(24.dp),
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.translate_pack_name),
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = packSizeLabel,
                            fontSize = 12.sp,
                            color = LimeGray,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    PackAction(
                        status = uiState.status,
                        onDownload = viewModel::download,
                        onDelete = { showDeleteConfirm = true },
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 翻译方式
            Text(
                text = stringResource(R.string.translate_mode_section),
                fontSize = 13.sp,
                color = LimeGray,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
            )
            SheetGroup(cardColor = MaterialTheme.colorScheme.surface) {
                TranslateMode.entries.forEachIndexed { index, modeItem ->
                    if (index > 0) SheetRowDivider(startIndent = 16.dp)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() },
                            ) { viewModel.setMode(modeItem) }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = when (modeItem) {
                                TranslateMode.Auto -> stringResource(R.string.translate_mode_auto)
                                TranslateMode.Offline -> stringResource(R.string.translate_mode_offline)
                            },
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f),
                        )
                        if (mode == modeItem) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 说明
            Text(
                text = stringResource(R.string.translate_pack_tips),
                fontSize = 12.sp,
                color = LimeGray,
                lineHeight = 18.sp,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }
    }

    // 删除确认弹窗
    if (showDeleteConfirm) {
        LimeAlertDialog(
            title = stringResource(R.string.translate_delete_pack_confirm_title),
            firstButtonText = stringResource(R.string.cancel),
            secondButtonText = stringResource(R.string.delete),
            onFirstButtonClick = { showDeleteConfirm = false },
            onSecondButtonClick = {
                showDeleteConfirm = false
                viewModel.delete()
            },
            onDismissRequest = { showDeleteConfirm = false },
        )
    }
}

/// 状态展示与操作按钮
@Composable
private fun PackAction(
    status: PackStatus,
    onDownload: () -> Unit,
    onDelete: () -> Unit,
) {
    when (status) {
        PackStatus.Checking -> LoadingLabel(stringResource(R.string.translate_checking))
        PackStatus.Downloading -> LoadingLabel(stringResource(R.string.translate_downloading))
        PackStatus.NotDownloaded -> Chip(
            text = stringResource(R.string.translate_download),
            background = MaterialTheme.colorScheme.primary,
            textColor = Color.White,
            onClick = onDownload,
        )

        PackStatus.Failed -> Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(text = stringResource(R.string.translate_download_failed), fontSize = 12.sp, color = Color(0xFFFF3B30))
            Chip(
                text = stringResource(R.string.translate_retry),
                background = MaterialTheme.colorScheme.primary,
                textColor = Color.White,
                onClick = onDownload,
            )
        }

        PackStatus.Downloaded -> Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.translate_downloaded),
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = stringResource(R.string.delete),
                fontSize = 13.sp,
                color = Color(0xFFFF3B30),
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { onDelete() }
                    .padding(horizontal = 4.dp, vertical = 2.dp),
            )
        }
    }
}

/// 圆角按钮
@Composable
private fun Chip(text: String, background: Color, textColor: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(background)
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = text, fontSize = 13.sp, color = textColor)
    }
}

/// 加载中标签
@Composable
private fun LoadingLabel(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(14.dp),
            color = MaterialTheme.colorScheme.primary,
            strokeWidth = 2.dp,
        )
        Text(text = text, fontSize = 12.sp, color = LimeGray)
    }
}
