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
import xyz.larkzhh.lime.core.designsystem.R as DesignSystemR
import xyz.larkzhh.lime.ui.components.LimeAlertDialog
import xyz.larkzhh.lime.ui.components.SheetGroup
import xyz.larkzhh.lime.ui.theme.LimeGray
import xyz.larkzhh.lime.ui.theme.LimeLightGray
import xyz.larkzhh.lime.util.text.AppLanguage
import xyz.larkzhh.lime.util.showToast

/// 语言设置子页
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageSettingsPage(
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    var selectedTag by remember { mutableStateOf(AppLanguage.currentTag()) }
    var showConfirm by remember { mutableStateOf(false) }
    val langSwitchedText = stringResource(R.string.lang_switched)
    val options = listOf(
        AppLanguage.TAG_SYSTEM,
        AppLanguage.TAG_SIMPLIFIED,
        AppLanguage.TAG_TRADITIONAL,
        AppLanguage.TAG_ENGLISH,
    )

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.settings_language),
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
                            if (selectedTag == AppLanguage.currentTag()) {
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
                options.forEachIndexed { index, tag ->
                    if (index > 0) {
                        Spacer(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(0.5.dp)
                                .padding(start = 16.dp)
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() },
                            ) { selectedTag = tag }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = languageDisplayName(tag),
                            fontSize = 16.sp,
                            color = if (tag == selectedTag) MaterialTheme.colorScheme.onSurface else LimeGray,
                            fontWeight = if (tag == selectedTag) FontWeight.Medium else FontWeight.Normal,
                            modifier = Modifier.weight(1f),
                        )
                        if (tag == selectedTag) {
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
            title = stringResource(R.string.language_confirm_title),
            text = stringResource(R.string.language_confirm_text),
            firstButtonText = stringResource(DesignSystemR.string.cancel),
            secondButtonText = stringResource(DesignSystemR.string.save),
            onFirstButtonClick = { showConfirm = false },
            onSecondButtonClick = {
                showConfirm = false
                AppLanguage.setTag(selectedTag)
                langSwitchedText.showToast(context)
                (context as? Activity)?.recreate()
            },
            onDismissRequest = { showConfirm = false },
        )
    }
}

/// 语言显示名
@Composable
fun languageDisplayName(tag: String): String = when (tag) {
    AppLanguage.TAG_SIMPLIFIED -> "简体中文"
    AppLanguage.TAG_TRADITIONAL -> "繁體中文"
    AppLanguage.TAG_ENGLISH -> "English"
    else -> stringResource(R.string.lang_system)
}
