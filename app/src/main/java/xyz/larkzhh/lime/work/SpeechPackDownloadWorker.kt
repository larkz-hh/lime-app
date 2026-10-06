package xyz.larkzhh.lime.work

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import xyz.larkzhh.lime.BuildConfig
import xyz.larkzhh.lime.R
import xyz.larkzhh.lime.data.local.SpeechModelStore
import xyz.larkzhh.lime.data.local.SpeechPackStatus

/**
 * 中文语音包后台下载任务
 */
class SpeechPackDownloadWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val store = SpeechModelStore(applicationContext, BuildConfig.SPEECH_MODEL_URL)
        if (store.isReady()) return Result.success()
        // 前台通知
        runCatching { setForeground(foregroundInfo(0)) }
        return coroutineScope {
            val progressJob = launch { collectProgress(store) }
            try {
                val success = store.download()
                if (success || store.isReady()) {
                    Result.success()
                } else if (runAttemptCount < MAX_ATTEMPTS) {
                    Result.retry()
                } else {
                    Result.failure()
                }
            } finally {
                progressJob.cancel()
            }
        }
    }

    /// 下载进度同步到通知
    private suspend fun collectProgress(store: SpeechModelStore) {
        var lastPercent = -1
        store.state.collect { state ->
            if (state.status != SpeechPackStatus.Downloading) return@collect
            // 每 5% 更新一次通知
            if (lastPercent >= 0 && state.progress - lastPercent < NOTIFY_STEP) return@collect
            lastPercent = state.progress
            runCatching { setForeground(foregroundInfo(state.progress)) }
        }
    }

    private fun foregroundInfo(progress: Int): ForegroundInfo {
        val context = applicationContext
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "离线包下载",
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "下载离线语音包"
                setShowBadge(false)
            },
        )
        val text = if (progress > 0) {
            context.getString(R.string.pack_downloading_percent, progress)
        } else {
            context.getString(R.string.translate_downloading)
        }
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_notification)
            .setContentTitle(context.getString(R.string.speech_pack_name))
            .setContentText(text)
            .setProgress(100, progress, progress <= 0)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .build()
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(NOTIFICATION_ID, notification)
        }
    }

    companion object {
        private const val UNIQUE_NAME = "speech_pack_download"
        private const val NOTIFICATION_ID = 4003
        private const val CHANNEL_ID = "offline_pack_download"
        private const val NOTIFY_STEP = 5
        private const val MAX_ATTEMPTS = 3

        /// 加入后台下载
        fun enqueue(context: Context) {
            WorkManager.getInstance(context).enqueueUniqueWork(
                UNIQUE_NAME,
                ExistingWorkPolicy.KEEP,
                buildRequest(),
            )
        }

        private fun buildRequest(): OneTimeWorkRequest =
            OneTimeWorkRequestBuilder<SpeechPackDownloadWorker>()
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build(),
                )
                .build()
    }
}
