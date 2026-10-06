package xyz.larkzhh.lime.ui.video.feed

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import xyz.larkzhh.lime.R
import xyz.larkzhh.lime.core.designsystem.R as DesignSystemR

/**
 * 视频流页 UI 文案
 */
class VideoFeedTexts internal constructor(
    val enableNotificationHint: String,
    val replyMenuLabel: String,
    val copyMenuLabel: String,
    val translateMenuLabel: String,
    val deleteMenuLabel: String,
    val copiedText: String,
    val backgroundDownloadToastText: String,
    val danmakuOffText: String,
    val danmakuOnText: String,
    val autoplayOffText: String,
    val autoplayOnText: String,
    val backgroundPlaybackOffText: String,
    val backgroundPlaybackOnText: String,
    val savingVideoText: String,
    val videoSavedToAlbumText: String,
    val videoSaveFailedText: String,
    val videoNoteDeletedText: String,
    val videoNoteDeleteFailedText: String,
    val speedChangedFormat: String,
)

@Composable
fun videoFeedTexts(): VideoFeedTexts = VideoFeedTexts(
    enableNotificationHint = stringResource(R.string.video_notify_settings_hint),
    replyMenuLabel = stringResource(R.string.video_reply),
    copyMenuLabel = stringResource(R.string.chat_copy),
    translateMenuLabel = stringResource(R.string.drawer_translate),
    deleteMenuLabel = stringResource(R.string.delete),
    copiedText = stringResource(R.string.copied),
    backgroundDownloadToastText = stringResource(R.string.translate_bg_download_toast),
    danmakuOffText = stringResource(R.string.video_danmaku_off),
    danmakuOnText = stringResource(R.string.video_danmaku_on),
    autoplayOffText = stringResource(R.string.video_autoplay_off),
    autoplayOnText = stringResource(R.string.video_autoplay_on),
    backgroundPlaybackOffText = stringResource(R.string.video_background_playback_off),
    backgroundPlaybackOnText = stringResource(R.string.video_background_playback_on),
    savingVideoText = stringResource(R.string.video_saving),
    videoSavedToAlbumText = stringResource(DesignSystemR.string.friend_saved_to_album),
    videoSaveFailedText = stringResource(DesignSystemR.string.friend_save_failed),
    videoNoteDeletedText = stringResource(R.string.video_note_deleted),
    videoNoteDeleteFailedText = stringResource(R.string.video_delete_note_failed),
    speedChangedFormat = stringResource(R.string.video_speed_changed),
)
