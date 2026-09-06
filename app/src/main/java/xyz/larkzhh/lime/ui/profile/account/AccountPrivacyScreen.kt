package xyz.larkzhh.lime.ui.profile.account

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import xyz.larkzhh.lime.R
import xyz.larkzhh.lime.ui.components.LimeAlertDialog
import xyz.larkzhh.lime.ui.components.LimeSwitch
import xyz.larkzhh.lime.ui.components.SheetGroup
import xyz.larkzhh.lime.ui.components.SheetRowDivider
import xyz.larkzhh.lime.ui.theme.LimeGray
import xyz.larkzhh.lime.ui.theme.LimeLightGray
import xyz.larkzhh.lime.ui.theme.LimePrimary
import xyz.larkzhh.lime.util.showToast


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountPrivacyScreen(
    onClose: () -> Unit,
    onPasswordChanged: () -> Unit,
    onLogout: () -> Unit,
    viewModel: AccountPrivacyViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var showForm by rememberSaveable { mutableStateOf(false) }

    // 打开页面重置表单状态，刷新邮箱
    LaunchedEffect(Unit) { viewModel.reset() }

    BackHandler {
        if (showForm) showForm = false else onClose()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // 账号与隐私主页
        AccountPrivacyRootPage(
            state = uiState,
            viewModel = viewModel,
            onBack = onClose,
            onChangePassword = { showForm = true },
            onLogout = onLogout,
        )
        // 修改密码页
        AnimatedVisibility(
            visible = showForm,
            enter = slideInHorizontally(initialOffsetX = { it }),
            exit = slideOutHorizontally(targetOffsetX = { it }),
        ) {
            ChangePasswordPage(
                state = uiState,
                viewModel = viewModel,
                onBack = { showForm = false },
                onPasswordChanged = onPasswordChanged,
            )
        }
    }
}

/// 账号与隐私主页
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccountPrivacyRootPage(
    state: AccountPrivacyUiState,
    viewModel: AccountPrivacyViewModel,
    onBack: () -> Unit,
    onChangePassword: () -> Unit,
    onLogout: () -> Unit,
) {
    val context = LocalContext.current
    var showLogoutConfirm by remember { mutableStateOf(false) }

    // 隐私设置失败提示
    LaunchedEffect(state.privacyError) {
        state.privacyError?.let { error ->
            error.showToast(context)
            viewModel.clearPrivacyError()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.drawer_account_privacy),
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
            SectionLabel(stringResource(R.string.account_section_account))
            SheetGroup(cardColor = Color.White) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Email,
                        contentDescription = null,
                        tint = Color(0xFF1C1C1E),
                        modifier = Modifier.size(24.dp),
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.account_login_email),
                            fontSize = 15.sp,
                            color = Color(0xFF1C1C1E),
                        )
                        Text(
                            text = state.email?.let { maskEmail(it) } ?: stringResource(R.string.account_email_unavailable),
                            fontSize = 12.sp,
                            color = LimeGray,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            SectionLabel(stringResource(R.string.account_section_security))
            SheetGroup(cardColor = Color.White) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() },
                            onClick = onChangePassword,
                        )
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Lock,
                        contentDescription = null,
                        tint = Color(0xFF1C1C1E),
                        modifier = Modifier.size(24.dp),
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = stringResource(R.string.account_change_password),
                        fontSize = 15.sp,
                        color = Color(0xFF1C1C1E),
                        modifier = Modifier.weight(1f),
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = LimeGray,
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            SectionLabel(stringResource(R.string.account_section_privacy))
            SheetGroup(cardColor = Color.White) {
                PrivacySwitchRow(
                    title = stringResource(R.string.account_like_list_title),
                    description = stringResource(R.string.account_like_list_desc),
                    checked = state.likePrivate,
                    enabled = !state.isPrivacySaving,
                    onCheckedChange = viewModel::setLikePrivate,
                )
                SheetRowDivider(startIndent = 16.dp)
                PrivacySwitchRow(
                    title = stringResource(R.string.account_fav_list_title),
                    description = stringResource(R.string.account_fav_list_desc),
                    checked = state.favPrivate,
                    enabled = !state.isPrivacySaving,
                    onCheckedChange = viewModel::setFavPrivate,
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = stringResource(R.string.account_change_password_note),
                fontSize = 12.sp,
                color = LimeGray,
                lineHeight = 18.sp,
                modifier = Modifier.padding(horizontal = 4.dp),
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 退出登录
            SheetGroup(cardColor = Color.White) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() },
                        ) { showLogoutConfirm = true }
                        .padding(horizontal = 16.dp, vertical = 15.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.account_logout),
                        fontSize = 16.sp,
                        color = Color(0xFFFF3B30),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }

    // 退出登录二次确认
    if (showLogoutConfirm) {
        LimeAlertDialog(
            title = stringResource(R.string.account_logout_confirm_title),
            firstButtonText = stringResource(R.string.cancel),
            secondButtonText = stringResource(R.string.account_logout),
            secondButtonColor = Color(0xFFFF3B30),
            onFirstButtonClick = { showLogoutConfirm = false },
            onSecondButtonClick = {
                showLogoutConfirm = false
                viewModel.logout(onLogout)
            },
            onDismissRequest = { showLogoutConfirm = false },
        )
    }
}

/// 隐私开关行
@Composable
private fun PrivacySwitchRow(
    title: String,
    description: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 15.sp,
                color = Color(0xFF1C1C1E),
            )
            Text(
                text = description,
                fontSize = 12.sp,
                color = LimeGray,
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        LimeSwitch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
        )
    }
}

/// 修改密码页
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChangePasswordPage(
    state: AccountPrivacyUiState,
    viewModel: AccountPrivacyViewModel,
    onBack: () -> Unit,
    onPasswordChanged: () -> Unit,
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.account_change_password),
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
                .imePadding()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // 验证方式
            SheetGroup(cardColor = Color.White) {
                VerifyMode.entries.forEachIndexed { index, mode ->
                    if (index > 0) SheetRowDivider(startIndent = 16.dp)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() },
                            ) { viewModel.setVerifyMode(mode) }
                            .padding(horizontal = 16.dp, vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(mode.labelRes),
                            fontSize = 15.sp,
                            color = if (state.verifyMode == mode) Color(0xFF1C1C1E) else LimeGray,
                            fontWeight = if (state.verifyMode == mode) FontWeight.Medium else FontWeight.Normal,
                            modifier = Modifier.weight(1f),
                        )
                        if (state.verifyMode == mode) {
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

            when (state.verifyMode) {
                VerifyMode.OldPassword -> {
                    PasswordField(
                        value = state.oldPassword,
                        onValueChange = viewModel::onOldPasswordChange,
                        label = stringResource(R.string.account_current_password),
                    )
                }

                VerifyMode.Code -> {
                    // 验证码发送至绑定邮箱
                    val emailAvailable = !state.email.isNullOrBlank()
                    val emailHint = state.email?.let { maskEmail(it) }
                    Text(
                        text = if (emailHint != null) {
                            stringResource(R.string.account_code_email_hint, emailHint)
                        } else {
                            stringResource(R.string.account_code_email_unavailable)
                        },
                        fontSize = 12.sp,
                        color = LimeGray,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        OutlinedTextField(
                            value = state.code,
                            onValueChange = viewModel::onCodeChange,
                            modifier = Modifier.weight(1f),
                            label = { Text(stringResource(R.string.account_verification_code)) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Next,
                            ),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = LimePrimary),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedButton(
                            onClick = viewModel::sendCode,
                            enabled = emailAvailable && state.sendCodeCountdown == 0 && !state.isSendingCode,
                            modifier = Modifier.height(56.dp),
                        ) {
                            when {
                                state.isSendingCode -> CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                )
                                state.sendCodeCountdown > 0 -> Text(
                                    "${state.sendCodeCountdown}s",
                                    style = MaterialTheme.typography.bodySmall,
                                )
                                else -> Text(stringResource(R.string.account_send_code), style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }

            PasswordField(
                value = state.newPassword,
                onValueChange = viewModel::onNewPasswordChange,
                label = stringResource(R.string.account_new_password),
                visible = state.passwordVisible,
                onToggleVisible = viewModel::togglePasswordVisible,
            )

            PasswordField(
                value = state.confirmPassword,
                onValueChange = viewModel::onConfirmPasswordChange,
                label = stringResource(R.string.account_confirm_new_password),
                visible = state.passwordVisible,
                onToggleVisible = viewModel::togglePasswordVisible,
            )

            Text(
                text = stringResource(R.string.account_password_rule),
                fontSize = 12.sp,
                color = LimeGray,
                modifier = Modifier.padding(horizontal = 4.dp),
            )

            if (state.errorMessage != null) {
                Text(
                    text = state.errorMessage,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
            }

            Button(
                onClick = viewModel::submitChangePassword,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                enabled = !state.isSubmitting,
            ) {
                if (state.isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = Color.White,
                    )
                } else {
                    Text(stringResource(R.string.account_change_password_submit), style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }

    // 修改成功，重新登录
    if (state.success) {
        AlertDialog(
            onDismissRequest = onPasswordChanged,
            title = { Text(stringResource(R.string.account_password_changed_title)) },
            text = { Text(stringResource(R.string.account_password_changed_message)) },
            confirmButton = {
                TextButton(onClick = onPasswordChanged) {
                    Text(stringResource(R.string.account_ok), color = LimePrimary, fontWeight = FontWeight.SemiBold)
                }
            },
        )
    }
}

/// 密码输入框
@Composable
private fun PasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    visible: Boolean = false,
    onToggleVisible: (() -> Unit)? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        leadingIcon = {
            Icon(
                imageVector = Icons.Filled.Lock,
                contentDescription = null,
                tint = LimeGray,
            )
        },
        trailingIcon = onToggleVisible?.let { toggle ->
            {
                IconButton(onClick = toggle) {
                    Icon(
                        imageVector = if (visible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                        contentDescription = if (visible) {
                            stringResource(R.string.account_password_hide)
                        } else {
                            stringResource(R.string.account_password_show)
                        },
                    )
                }
            }
        },
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Next,
        ),
        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = LimePrimary),
    )
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

/// 邮箱脱敏
private fun maskEmail(email: String): String {
    if ('*' in email) return email
    val at = email.indexOf('@')
    if (at <= 0) return email
    val local = email.substring(0, at)
    val domain = email.substring(at)
    val prefix = local.take(3)
    return "$prefix***$domain"
}
