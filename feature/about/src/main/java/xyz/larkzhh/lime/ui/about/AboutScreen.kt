package xyz.larkzhh.lime.ui.about

import android.content.Context
import android.content.Intent
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.SystemUpdateAlt
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import xyz.larkzhh.lime.core.designsystem.R as DesignSystemR
import xyz.larkzhh.lime.feature.about.R
import xyz.larkzhh.lime.core.designsystem.components.LimeAlertDialog
import xyz.larkzhh.lime.core.designsystem.components.SheetGroup
import xyz.larkzhh.lime.ui.theme.LimeGray
import xyz.larkzhh.lime.ui.theme.LimeLightGray
import xyz.larkzhh.lime.util.copyToClipboard
import xyz.larkzhh.lime.util.showToast
import androidx.core.net.toUri

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    onClose: () -> Unit,
    viewModel: AboutViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    var pendingRelease by remember { mutableStateOf<ReleaseInfo?>(null) }
    var checking by remember { mutableStateOf(false) }
    val aboutLatestText = stringResource(R.string.about_latest)
    val emailCopiedText = stringResource(R.string.about_email_copied)
    val installPermissionText = stringResource(R.string.about_install_permission_needed)
    val aboutOpenUrlFailedText = stringResource(R.string.about_open_url_failed)

    BackHandler { onClose() }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.about_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(DesignSystemR.string.back))
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
                .padding(top = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // 图标与版本
            Spacer(modifier = Modifier.height(16.dp))
            Icon(
                painter = painterResource(DesignSystemR.drawable.app_logo),
                contentDescription = stringResource(R.string.about_app_icon),
                tint = Color.Unspecified,
                modifier = Modifier.size(84.dp),
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Lime",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = viewModel.versionLabel,
                fontSize = 13.sp,
                color = LimeGray,
            )

            Spacer(modifier = Modifier.height(28.dp))

            // 检查更新
            Column(modifier = Modifier.fillMaxWidth()) {
                SheetGroup(cardColor = MaterialTheme.colorScheme.surface) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() },
                            ) {
                                if (!checking) {
                                    checking = true
                                    viewModel.checkUpdate { result ->
                                        checking = false
                                        when (result) {
                                            is UpdateCheckResult.Latest ->
                                                aboutLatestText.showToast(context)
                                            is UpdateCheckResult.Found ->
                                                pendingRelease = result.release
                                            is UpdateCheckResult.Failed ->
                                                result.message.showToast(context)
                                        }
                                    }
                                }
                            }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.SystemUpdateAlt,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.width(24.dp),
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = stringResource(R.string.about_check_update),
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f),
                        )
                        if (checking) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        } else {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = LimeGray,
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 作者与项目信息
            val links = buildList {
                if (AboutConfig.REPO.isNotBlank()) {
                    add(AboutLink(Icons.Outlined.Code, stringResource(R.string.about_repo), AboutConfig.REPO) {
                        openUrl(context, "https://github.com/${AboutConfig.REPO}", aboutOpenUrlFailedText)
                    })
                }
                if (AboutConfig.AUTHOR_GITHUB.isNotBlank()) {
                    add(AboutLink(Icons.Outlined.Person, stringResource(R.string.about_author_github), "@${AboutConfig.AUTHOR_GITHUB}") {
                        openUrl(context, "https://github.com/${AboutConfig.AUTHOR_GITHUB}", aboutOpenUrlFailedText)
                    })
                }
                if (AboutConfig.AUTHOR_EMAIL.isNotBlank()) {
                    add(AboutLink(Icons.Outlined.Email, stringResource(R.string.about_qq_email), AboutConfig.AUTHOR_EMAIL) {
                        AboutConfig.AUTHOR_EMAIL.copyToClipboard(context)
                        emailCopiedText.showToast(context)
                    })
                }
                if (AboutConfig.HOMEPAGE.isNotBlank()) {
                    add(AboutLink(Icons.Outlined.Language, stringResource(R.string.about_homepage), AboutConfig.HOMEPAGE) {
                        openUrl(context, AboutConfig.HOMEPAGE, aboutOpenUrlFailedText)
                    })
                }
            }
            if (links.isNotEmpty()) {
                SheetGroup(cardColor = MaterialTheme.colorScheme.surface) {
                    links.forEachIndexed { index, link ->
                        if (index > 0) {
                            HorizontalDivider(
                                modifier = Modifier.padding(start = 16.dp),
                                color = Color(0xFFE5E5E5),
                                thickness = 0.5.dp,
                            )
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(
                                    indication = null,
                                    interactionSource = remember { MutableInteractionSource() },
                                    onClick = link.onClick,
                                )
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = link.icon,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.width(24.dp),
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = link.label,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                text = link.value,
                                fontSize = 13.sp,
                                color = LimeGray,
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = LimeGray,
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // 发现新版本弹窗
    val release = pendingRelease
    if (release != null) {
        LimeAlertDialog(
            title = stringResource(R.string.about_new_version_found, release.versionName),
            text = release.notes.take(500).ifBlank { stringResource(R.string.about_update_prompt) },
            firstButtonText = stringResource(R.string.about_update_later),
            secondButtonText = stringResource(R.string.about_update_now),
            onFirstButtonClick = { pendingRelease = null },
            onSecondButtonClick = {
                pendingRelease = null
                viewModel.startUpdate(
                    release = release,
                    onNeedPermission = {
                        installPermissionText.showToast(context)
                        viewModel.openInstallSettings()
                    },
                    onFailed = { it.showToast(context) },
                )
            },
            onDismissRequest = { pendingRelease = null },
        )
    }
}

private data class AboutLink(
    val icon: ImageVector,
    val label: String,
    val value: String,
    val onClick: () -> Unit,
)

private fun openUrl(context: Context, url: String, openFailedText: String) {
    runCatching {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, url.toUri()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }.onFailure { openFailedText.showToast(context) }
}

