package xyz.larkzhh.lime.ui.publish.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import xyz.larkzhh.lime.feature.publish.R
import xyz.larkzhh.lime.navigation.route.Screen
import xyz.larkzhh.lime.ui.components.LimeAlertDialog
import xyz.larkzhh.lime.ui.publish.ai.AiWriteAction
import xyz.larkzhh.lime.ui.theme.LimeWhite
import xyz.larkzhh.lime.util.system.findActivity
import xyz.larkzhh.lime.util.showToast
import xyz.larkzhh.lime.core.designsystem.R as DesignSystemR

/**
 * 图文、视频发布页共享的表单骨架
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotePublishScaffold(
    navController: NavHostController,
    topBarTitle: String,
    title: String,
    content: String,
    onTitleChange: (String) -> Unit,
    onContentChange: (String) -> Unit,
    isPublishing: Boolean,
    error: String?,
    progressText: String?,
    isSuccess: Boolean,
    isDraftSuccess: Boolean,
    onClearSuccess: () -> Unit,
    onClearDraftSuccess: () -> Unit,
    onSaveDraft: () -> Unit,
    onPublish: () -> Unit,
    onAiAssist: () -> Unit,
    onAiAction: (AiWriteAction) -> Unit,
    hasImages: Boolean,
    isEdit: Boolean = false,// 编辑模式
    onDraftSaved: (() -> Unit)? = null,
    topContent: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val savedEditToast = stringResource(R.string.publish_saved_edit)
    val draftSavedToast = stringResource(R.string.publish_draft_saved)
    var showDraftDialog by remember { mutableStateOf(false) }
    val aiEnabled = (content.isNotBlank() || hasImages) && !isPublishing// 有图或有正文、未发布中

    // 编辑模式返回直接回到发起页
    val goBack: () -> Unit = {
        if (isEdit) {
            val popped = navController.popBackStack(Screen.Publish.route, inclusive = true)
            if (!popped) navController.popBackStack()
        } else {
            navController.popBackStack()
        }
    }
    BackHandler(enabled = isEdit, onBack = goBack)

    // 发布或编辑成功后直接回首页
    LaunchedEffect(isSuccess) {
        if (isSuccess) {
            onClearSuccess()
            if (isEdit) {
                savedEditToast.showToast(context)
            }
            try {
                navController.navigate(Screen.Home.route) {
                    popUpTo(Screen.Home.route) { inclusive = false }
                }
            } catch (e: IllegalArgumentException) {
                (context.findActivity())?.finish()
            }
        }
    }

    // 存草稿成功后 Toast 并返回
    LaunchedEffect(isDraftSuccess) {
        if (isDraftSuccess) {
            onClearDraftSuccess()
            draftSavedToast.showToast(context)
            if (onDraftSaved != null) {
                onDraftSaved()
            } else {
                try {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = false }
                    }
                } catch (e: IllegalArgumentException) {
                    context.findActivity()?.finish()
                }
            }
        }
    }

    // 存草稿确认弹窗
    if (showDraftDialog) {
        LimeAlertDialog(
            title = stringResource(R.string.publish_confirm_draft_title),
            firstButtonText = stringResource(DesignSystemR.string.cancel),
            secondButtonText = stringResource(R.string.publish_action_draft),
            onFirstButtonClick = { showDraftDialog = false },
            onSecondButtonClick = {
                showDraftDialog = false
                onSaveDraft()
            },
            onDismissRequest = { showDraftDialog = false },
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        // 顶栏
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = goBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(DesignSystemR.string.back))
            }
            Text(
                text = topBarTitle,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            // AI 帮写入口，有图或正文非空、且非发布中时可用
            IconButton(
                onClick = onAiAssist,
                enabled = aiEnabled,
            ) {
                Icon(
                    painterResource(R.drawable.ic_ai),
                    contentDescription = stringResource(R.string.publish_ai_write),
                    tint = if (aiEnabled) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
                    modifier = Modifier.size(20.dp),
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            // 顶部插槽（图片或视频）
            topContent()

            // 标题输入
            OutlinedTextField(
                value = title,
                onValueChange = onTitleChange,
                placeholder = {
                    Text(
                        stringResource(R.string.publish_add_title_hint),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.32f),
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                maxLines = 2,
                textStyle = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = Color.Transparent,
                    focusedBorderColor = Color.Transparent,
                ),
            )

            // 正文输入
            OutlinedTextField(
                value = content,
                onValueChange = onContentChange,
                placeholder = {
                    Text(
                        stringResource(R.string.publish_add_content_hint),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.24f),
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 140.dp)
                    .padding(horizontal = 8.dp),
                maxLines = 20,
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = Color.Transparent,
                    focusedBorderColor = Color.Transparent,
                ),
            )
        }

        // AI 帮写动作条
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                painterResource(R.drawable.ic_ai),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = stringResource(R.string.publish_ai_write),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            AiWriteAction.visibleFor(hasImages).forEach { action ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                        .clickable(enabled = aiEnabled) { onAiAction(action) }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                ) {
                    Text(
                        text = stringResource(action.labelRes),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (aiEnabled) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                    )
                }
            }
        }

        // 错误提示
        if (error != null) {
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
        }

        // 上传进度提示
        if (isPublishing && progressText != null) {
            Text(
                text = progressText,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
        }

        // 底部栏
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // 存草稿
            Button(
                onClick = { showDraftDialog = true },
                enabled = !isPublishing,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    disabledContainerColor = Color.White.copy(alpha = 0.6f),
                    disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
            ) {
                Text(stringResource(R.string.publish_action_draft), fontWeight = FontWeight.SemiBold)
            }
            // 发布、保存修改
            Button(
                onClick = onPublish,
                enabled = !isPublishing,
                modifier = Modifier.weight(2f),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            ) {
                if (isPublishing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = Color.White,
                    )
                } else {
                    Text(stringResource(R.string.publish_action_publish), fontWeight = FontWeight.SemiBold)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}
