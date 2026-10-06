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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import xyz.larkzhh.lime.feature.settings.R
import xyz.larkzhh.lime.core.designsystem.R as DesignSystemR
import xyz.larkzhh.lime.core.designsystem.components.LimeAlertDialog
import xyz.larkzhh.lime.core.designsystem.components.SheetGroup
import xyz.larkzhh.lime.core.theme.AppFont
import xyz.larkzhh.lime.ui.theme.FontOption
import xyz.larkzhh.lime.ui.theme.LimeGray
import xyz.larkzhh.lime.ui.theme.LimeLightGray
import xyz.larkzhh.lime.ui.theme.fontDisplayName
import xyz.larkzhh.lime.ui.theme.fontFamilyOf
import xyz.larkzhh.lime.util.showToast

/// 字体设置子页
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FontSettingsPage(
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    var selectedTag by remember { mutableStateOf(AppFont.currentTag()) }
    var showConfirm by remember { mutableStateOf(false) }
    val fontSwitchedText = stringResource(R.string.font_switched)
    val options = FontOption.entries

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.font_settings_title),
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
                            if (selectedTag == AppFont.currentTag()) {
                                onBack()
                            } else {
                                showConfirm = true
                            }
                        },
                    ) {
                        Text(
                            text = stringResource(DesignSystemR.string.save),
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
                options.forEachIndexed { index, option ->
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
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // 用该字体渲染名字做预览
                        Text(
                            text = fontDisplayName(option),
                            fontFamily = fontFamilyOf(option, context) ?: FontFamily.Default,
                            fontSize = 18.sp,
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
            title = stringResource(R.string.font_confirm_title),
            text = stringResource(R.string.font_confirm_text),
            firstButtonText = stringResource(DesignSystemR.string.cancel),
            secondButtonText = stringResource(DesignSystemR.string.save),
            onFirstButtonClick = { showConfirm = false },
            onSecondButtonClick = {
                showConfirm = false
                AppFont.setTag(selectedTag)
                fontSwitchedText.showToast(context)
                (context as? Activity)?.recreate()
            },
            onDismissRequest = { showConfirm = false },
        )
    }
}

/// 当前字体显示名
@Composable
fun currentFontDisplayName(): String = fontDisplayName(FontOption.fromTag(AppFont.currentTag()))
