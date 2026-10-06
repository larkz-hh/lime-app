package xyz.larkzhh.lime.ui.detail

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import xyz.larkzhh.lime.core.designsystem.R as DesignSystemR
import xyz.larkzhh.lime.feature.detail.R

/**
 * 笔记详情页 UI 文案
 */
class DetailTexts internal constructor(
    val copiedToast: String,
    val imageVoiceMutexToast: String,
    val voiceOnlyToast: String,
    val deleteFailedToast: String,
    val noteDeletedToast: String,
    val qrSavedToast: String,
    val saveFailedToast: String,
    val qrGenFailedToast: String,
    val bgDownloadAddedToast: String,
    val replyMenuLabel: String,
    val copyMenuLabel: String,
    val translateMenuLabel: String,
    val deleteMenuLabel: String,
)

@Composable
fun detailTexts(): DetailTexts = DetailTexts(
    copiedToast = stringResource(R.string.detail_copied),
    imageVoiceMutexToast = stringResource(DesignSystemR.string.comment_image_voice_mutex),
    voiceOnlyToast = stringResource(DesignSystemR.string.comment_voice_single),
    deleteFailedToast = stringResource(R.string.detail_delete_failed),
    noteDeletedToast = stringResource(R.string.detail_note_deleted),
    qrSavedToast = stringResource(DesignSystemR.string.detail_qr_saved),
    saveFailedToast = stringResource(DesignSystemR.string.detail_save_failed),
    qrGenFailedToast = stringResource(DesignSystemR.string.detail_qr_generate_failed),
    bgDownloadAddedToast = stringResource(R.string.detail_translate_bg_download),
    replyMenuLabel = stringResource(R.string.comment_reply_action),
    copyMenuLabel = stringResource(DesignSystemR.string.chat_copy),
    translateMenuLabel = stringResource(DesignSystemR.string.drawer_translate),
    deleteMenuLabel = stringResource(DesignSystemR.string.delete),
)
