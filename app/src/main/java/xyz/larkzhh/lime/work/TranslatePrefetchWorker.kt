package xyz.larkzhh.lime.work

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.google.mlkit.common.model.DownloadConditions
import com.tencent.mmkv.MMKV
import kotlinx.coroutines.CancellationException
import xyz.larkzhh.lime.data.local.TranslatorHolder

/**
 * 中英离线翻译语言包后台任务
 */
class TranslatePrefetchWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val wifiOnly = inputData.getBoolean(KEY_WIFI_ONLY, true)
        val conditions = if (wifiOnly) {
            DownloadConditions.Builder().requireWifi().build()
        } else {
            DownloadConditions.Builder().build()
        }
        return try {
            val holder = TranslatorHolder()
            holder.ensureModel("zh", "en", conditions)
            holder.ensureModel("en", "zh", conditions)
            // 打上一次性标记：以后启动不再重复唤醒
            MMKV.defaultMMKV().encode(KEY_PREFETCH_DONE, true)
            Result.success()
        } catch (ce: CancellationException) {
            throw ce
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        private const val UNIQUE_NAME = "translate_model_prefetch"
        private const val KEY_WIFI_ONLY = "wifi_only"
        private const val KEY_PREFETCH_DONE = "translate_pack_prefetched"

        /// 首次启动预下载
        fun enqueueOnFirstLaunch(context: Context) {
            if (MMKV.defaultMMKV().decodeBool(KEY_PREFETCH_DONE, false)) return
            WorkManager.getInstance(context).enqueueUniqueWork(
                UNIQUE_NAME,
                ExistingWorkPolicy.KEEP,
                buildRequest(wifiOnly = true),
            )
        }

        /// 用户手动下载
        fun enqueueBackgroundDownload(context: Context) {
            WorkManager.getInstance(context).enqueueUniqueWork(
                UNIQUE_NAME,
                ExistingWorkPolicy.REPLACE,
                buildRequest(wifiOnly = false),
            )
        }

        private fun buildRequest(wifiOnly: Boolean): OneTimeWorkRequest =
            OneTimeWorkRequestBuilder<TranslatePrefetchWorker>()
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(
                            if (wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED,
                        )
                        .build(),
                )
                .setInputData(workDataOf(KEY_WIFI_ONLY to wifiOnly))
                .build()
    }
}
