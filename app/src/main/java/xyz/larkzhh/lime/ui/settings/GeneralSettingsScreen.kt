package xyz.larkzhh.lime.ui.settings

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.NotificationsActive
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import xyz.larkzhh.lime.R
import xyz.larkzhh.lime.ui.components.LimeAlertDialog
import xyz.larkzhh.lime.ui.components.LimeSwitch
import xyz.larkzhh.lime.ui.components.SheetGroup
import xyz.larkzhh.lime.ui.theme.LimeGray
import xyz.larkzhh.lime.ui.theme.LimeLightGray
import xyz.larkzhh.lime.ui.theme.LimePrimary
import xyz.larkzhh.lime.util.AppLanguage
import xyz.larkzhh.lime.util.showToast

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeneralSettingsScreen(
    onClose: () -> Unit,
    viewModel: GeneralSettingsViewModel = hiltViewModel(),
) {
    val notifyEnabled by viewModel.notifyEnabled.collectAsState()
    val cacheLabel by viewModel.cacheLabel.collectAsState()
    var showLanguagePage by rememberSaveable { mutableStateOf(false) }
    var showClearDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val cacheClearedTemplate = stringResource(R.string.settings_cache_cleared)

    // 刷新缓存大小
    LaunchedEffect(Unit) { viewModel.refreshCacheSize() }

    BackHandler {
        if (showLanguagePage) showLanguagePage = false else onClose()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // 通用主页
        GeneralSettingsRootPage(
            notifyEnabled = notifyEnabled,
            cacheLabel = cacheLabel,
            viewModel = viewModel,
            onClose = onClose,
            onOpenLanguage = { showLanguagePage = true },
            onClearCache = { showClearDialog = true },
        )
        // 语言设置子页
        AnimatedVisibility(
            visible = showLanguagePage,
            enter = slideInHorizontally(initialOffsetX = { it }),
            exit = slideOutHorizontally(targetOffsetX = { it }),
        ) {
            LanguageSettingsPage(onBack = { showLanguagePage = false })
        }
    }

    // 清空缓存二次确认
    if (showClearDialog) {
        LimeAlertDialog(
            title = stringResource(R.string.settings_clear_cache_confirm_title),
            firstButtonText = stringResource(R.string.cancel),
            secondButtonText = stringResource(R.string.settings_clear),
            secondButtonColor = Color(0xFFFF3B30),
            onFirstButtonClick = { showClearDialog = false },
            onSecondButtonClick = {
                showClearDialog = false
                viewModel.clearCache { label ->
                    cacheClearedTemplate.replace("%1\$s", label).showToast(context)
                }
            },
            onDismissRequest = { showClearDialog = false },
        )
    }
}

/// 通用设置主页
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GeneralSettingsRootPage(
    notifyEnabled: Boolean,
    cacheLabel: String,
    viewModel: GeneralSettingsViewModel,
    onClose: () -> Unit,
    onOpenLanguage: () -> Unit,
    onClearCache: () -> Unit,
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.settings_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                        )
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
            SectionLabel(stringResource(R.string.settings_section_notify))

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
                        text = stringResource(R.string.settings_notify_reminders),
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

            SectionLabel(stringResource(R.string.settings_section_language))

            SheetGroup(cardColor = Color.White) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() },
                        ) { onOpenLanguage() }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Language,
                        contentDescription = null,
                        tint = Color(0xFF1C1C1E),
                        modifier = Modifier.width(24.dp),
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = stringResource(R.string.settings_language),
                        fontSize = 15.sp,
                        color = Color(0xFF1C1C1E),
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = languageDisplayName(AppLanguage.currentTag()),
                        fontSize = 14.sp,
                        color = LimeGray,
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            SectionLabel(stringResource(R.string.settings_section_storage))

            SheetGroup(cardColor = Color.White) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() },
                        ) { onClearCache() }
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
                        text = stringResource(R.string.settings_clear_cache),
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
}

/// 语言设置子页
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LanguageSettingsPage(
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
                            contentDescription = stringResource(R.string.back),
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
                            text = stringResource(R.string.save),
                            fontSize = 15.sp,
                            color = LimePrimary,
                            fontWeight = FontWeight.Medium,
                        )
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
                .padding(horizontal = 16.dp)
                .padding(top = 12.dp),
        ) {
            SheetGroup(cardColor = Color.White) {
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
                            color = if (tag == selectedTag) Color(0xFF1C1C1E) else LimeGray,
                            fontWeight = if (tag == selectedTag) FontWeight.Medium else FontWeight.Normal,
                            modifier = Modifier.weight(1f),
                        )
                        if (tag == selectedTag) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                                tint = LimePrimary,
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
            firstButtonText = stringResource(R.string.cancel),
            secondButtonText = stringResource(R.string.save),
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
private fun languageDisplayName(tag: String): String = when (tag) {
    AppLanguage.TAG_SIMPLIFIED -> "简体中文"
    AppLanguage.TAG_TRADITIONAL -> "繁體中文"
    AppLanguage.TAG_ENGLISH -> "English"
    else -> stringResource(R.string.lang_system)
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
