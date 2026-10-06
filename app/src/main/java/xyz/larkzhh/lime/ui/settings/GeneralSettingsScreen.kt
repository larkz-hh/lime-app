package xyz.larkzhh.lime.ui.settings

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Autorenew
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.TextFields
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
import androidx.compose.ui.graphics.vector.ImageVector
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
import xyz.larkzhh.lime.ui.components.SheetRowDivider
import xyz.larkzhh.lime.ui.theme.LimeGray
import xyz.larkzhh.lime.util.text.AppLanguage
import xyz.larkzhh.lime.ui.AppSplashAnim
import xyz.larkzhh.lime.util.showToast
import xyz.larkzhh.lime.core.designsystem.R as DesignSystemR

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeneralSettingsScreen(
    onClose: () -> Unit,
    viewModel: GeneralSettingsViewModel = hiltViewModel(),
) {
    val notifyEnabled by viewModel.notifyEnabled.collectAsState()
    val cacheLabel by viewModel.cacheLabel.collectAsState()
    var splashAnimEnabled by remember { mutableStateOf(AppSplashAnim.enabled()) }
    var showLanguagePage by remember { mutableStateOf(false) }
    var showThemePage by remember { mutableStateOf(false) }
    var showFontPage by remember { mutableStateOf(false) }
    var showDarkModePage by remember { mutableStateOf(false) }
    var showClearDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val cacheClearedTemplate = stringResource(R.string.settings_cache_cleared)

    // 刷新缓存大小
    LaunchedEffect(Unit) { viewModel.refreshCacheSize() }

    BackHandler {
        when {
            showLanguagePage -> showLanguagePage = false
            showThemePage -> showThemePage = false
            showFontPage -> showFontPage = false
            showDarkModePage -> showDarkModePage = false
            else -> onClose()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // 通用主页
        GeneralSettingsRootPage(
            notifyEnabled = notifyEnabled,
            cacheLabel = cacheLabel,
            onClose = onClose,
            onNotifyChange = viewModel::setNotifyEnabled,
            splashAnimEnabled = splashAnimEnabled,
            onSplashAnimChange = { value ->
                splashAnimEnabled = value
                AppSplashAnim.setEnabled(value)
            },
            onOpenLanguage = { showLanguagePage = true },
            onOpenTheme = { showThemePage = true },
            onOpenFont = { showFontPage = true },
            onOpenDarkMode = { showDarkModePage = true },
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
        // 主题设置子页
        AnimatedVisibility(
            visible = showThemePage,
            enter = slideInHorizontally(initialOffsetX = { it }),
            exit = slideOutHorizontally(targetOffsetX = { it }),
        ) {
            ThemeSettingsPage(onBack = { showThemePage = false })
        }
        // 字体设置子页
        AnimatedVisibility(
            visible = showFontPage,
            enter = slideInHorizontally(initialOffsetX = { it }),
            exit = slideOutHorizontally(targetOffsetX = { it }),
        ) {
            FontSettingsPage(onBack = { showFontPage = false })
        }
        // 深色模式子页
        AnimatedVisibility(
            visible = showDarkModePage,
            enter = slideInHorizontally(initialOffsetX = { it }),
            exit = slideOutHorizontally(targetOffsetX = { it }),
        ) {
            DarkModeSettingsPage(onBack = { showDarkModePage = false })
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
    onClose: () -> Unit,
    onNotifyChange: (Boolean) -> Unit,
    splashAnimEnabled: Boolean,
    onSplashAnimChange: (Boolean) -> Unit,
    onOpenLanguage: () -> Unit,
    onOpenTheme: () -> Unit,
    onOpenFont: () -> Unit,
    onOpenDarkMode: () -> Unit,
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
                            contentDescription = stringResource(DesignSystemR.string.back),
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(top = 12.dp),
        ) {
            SectionLabel(stringResource(R.string.settings_section_notify))

            SheetGroup(cardColor = MaterialTheme.colorScheme.surface) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.NotificationsActive,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.width(24.dp),
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = stringResource(R.string.settings_notify_reminders),
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    LimeSwitch(
                        checked = notifyEnabled,
                        onCheckedChange = onNotifyChange,
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 显示：语言 / 字体 / 深色模式 / 主题
            SectionLabel(stringResource(R.string.settings_section_display))

            SheetGroup(cardColor = MaterialTheme.colorScheme.surface) {
                SettingsEntryRow(
                    icon = Icons.Outlined.Language,
                    title = stringResource(R.string.settings_language),
                    value = languageDisplayName(AppLanguage.currentTag()),
                    onClick = onOpenLanguage,
                )
                SheetRowDivider(startIndent = 16.dp)
                SettingsEntryRow(
                    icon = Icons.Outlined.TextFields,
                    title = stringResource(R.string.settings_font),
                    value = currentFontDisplayName(),
                    onClick = onOpenFont,
                )
                SheetRowDivider(startIndent = 16.dp)
                SettingsEntryRow(
                    icon = Icons.Outlined.DarkMode,
                    title = stringResource(R.string.settings_dark_mode),
                    value = currentDarkModeDisplayName(),
                    onClick = onOpenDarkMode,
                )
                SheetRowDivider(startIndent = 16.dp)
                SettingsEntryRow(
                    icon = Icons.Outlined.Palette,
                    title = stringResource(R.string.settings_theme),
                    value = stringResource(themeDisplayRes()),
                    onClick = onOpenTheme,
                )
                SheetRowDivider(startIndent = 16.dp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Autorenew,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.width(24.dp),
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = stringResource(R.string.settings_splash_animation),
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    LimeSwitch(
                        checked = splashAnimEnabled,
                        onCheckedChange = onSplashAnimChange,
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            SectionLabel(stringResource(R.string.settings_section_storage))

            SheetGroup(cardColor = MaterialTheme.colorScheme.surface) {
                SettingsEntryRow(
                    icon = Icons.Outlined.DeleteOutline,
                    title = stringResource(R.string.settings_clear_cache),
                    value = cacheLabel,
                    onClick = onClearCache,
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

/// 设置入口行（图标 + 标题 + 当前值）
@Composable
private fun SettingsEntryRow(
    icon: ImageVector,
    title: String,
    value: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
            ) { onClick() }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.width(24.dp),
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title,
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = value,
            fontSize = 14.sp,
            color = LimeGray,
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
