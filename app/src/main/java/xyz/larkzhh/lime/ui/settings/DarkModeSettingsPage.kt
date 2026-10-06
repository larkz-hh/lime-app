package xyz.larkzhh.lime.ui.settings

import android.app.Activity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import xyz.larkzhh.lime.R
import xyz.larkzhh.lime.ui.components.LimeAlertDialog
import xyz.larkzhh.lime.ui.components.SheetGroup
import xyz.larkzhh.lime.ui.theme.AppDarkMode
import xyz.larkzhh.lime.ui.theme.DarkModeOption
import xyz.larkzhh.lime.ui.theme.LimeGray
import xyz.larkzhh.lime.util.showToast
import xyz.larkzhh.lime.core.designsystem.R as DesignSystemR

/// 深色模式设置子页
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DarkModeSettingsPage(
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    var selectedTag by remember { mutableStateOf(AppDarkMode.currentTag()) }
    var showConfirm by remember { mutableStateOf(false) }
    val darkModeSwitchedText = stringResource(R.string.dark_mode_switched)

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.settings_dark_mode),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(DesignSystemR.string.back),
                        )
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            // 未修改时直接返回
                            if (selectedTag == AppDarkMode.currentTag()) {
                                onBack()
                            } else {
                                showConfirm = true
                            }
                        },
                    ) {
                        Text(
                            text = stringResource(R.string.save),
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = Color.Unspecified,
                    navigationIconContentColor = Color.Unspecified,
                    titleContentColor = Color.Unspecified,
                    actionIconContentColor = Color.Unspecified,
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
            SheetGroup(cardColor = MaterialTheme.colorScheme.surface) {
                DarkModeOption.entries.forEachIndexed { index, option ->
                    if (index > 0) {
                        Spacer(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(0.5.dp)
                                .padding(start = 16.dp)
                        )
                    }
                    val selected = option.tag == selectedTag
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() },
                            ) { selectedTag = option.tag }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = darkModeOptionLabel(option),
                            fontSize = 16.sp,
                            color = if (selected) MaterialTheme.colorScheme.onSurface else LimeGray,
                            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
                            modifier = Modifier.weight(1f),
                        )
                        if (selected) {
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
        }
    }

    // 保存确认
    if (showConfirm) {
        LimeAlertDialog(
            title = stringResource(R.string.dark_mode_confirm_title),
            text = stringResource(R.string.dark_mode_confirm_text),
            firstButtonText = stringResource(R.string.cancel),
            secondButtonText = stringResource(R.string.save),
            onFirstButtonClick = { showConfirm = false },
            onSecondButtonClick = {
                showConfirm = false
                AppDarkMode.setTag(selectedTag)
                darkModeSwitchedText.showToast(context)
                (context as? Activity)?.recreate()
            },
            onDismissRequest = { showConfirm = false },
        )
    }
}

/// 深色模式选项文案
@Composable
fun darkModeOptionLabel(option: DarkModeOption): String = when (option) {
    DarkModeOption.SYSTEM -> stringResource(R.string.dark_mode_option_system)
    DarkModeOption.DARK -> stringResource(R.string.dark_mode_option_dark)
}

/// 当前深色模式显示名
@Composable
fun currentDarkModeDisplayName(): String = darkModeOptionLabel(DarkModeOption.fromTag(AppDarkMode.currentTag()))
