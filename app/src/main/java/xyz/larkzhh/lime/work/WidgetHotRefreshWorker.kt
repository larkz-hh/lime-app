package xyz.larkzhh.lime.work

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import xyz.larkzhh.lime.ui.widget.WidgetHotCache
import xyz.larkzhh.lime.ui.widget.WidgetHotRefresher
import java.util.concurrent.TimeUnit

/**
 * 桌面小组件热搜定时刷新任务
 */
class WidgetHotRefreshWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        if (!WidgetHotCache.shouldRefresh()) return Result.success()

        return if (WidgetHotRefresher.refresh(applicationContext)) {
            Result.success()
        } else {
            Result.retry()
        }
    }

    companion object {
        private const val UNIQUE_NAME = "widget_hot_refresh"
        private const val PERIOD_MINUTES = 15L

        fun ensureScheduled(context: Context) {
            val request = PeriodicWorkRequestBuilder<WidgetHotRefreshWorker>(
                PERIOD_MINUTES,
                TimeUnit.MINUTES,
            )
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build(),
                )
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }
    }
}
