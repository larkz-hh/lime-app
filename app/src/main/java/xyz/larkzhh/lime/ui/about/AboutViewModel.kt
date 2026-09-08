package xyz.larkzhh.lime.ui.about

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import androidx.core.content.pm.PackageInfoCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject
import xyz.larkzhh.lime.R

/// 检查更新结果
sealed interface UpdateCheckResult {
    data class Latest(val versionLabel: String) : UpdateCheckResult
    data class Found(val release: ReleaseInfo) : UpdateCheckResult
    data class Failed(val message: String) : UpdateCheckResult
}

/**
 * 关于页 ViewModel
 */
@HiltViewModel
class AboutViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val updater: AppUpdater,
) : ViewModel() {

    /// 当前版本
    val versionLabel: String by lazy {
        runCatching {
            val info = context.packageManager.getPackageInfo(context.packageName, 0)
            "${info.versionName}(${PackageInfoCompat.getLongVersionCode(info)})"
        }.getOrDefault("")
    }

    private var receiverRegistered = false

    private val downloadReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            if (intent?.action == DownloadManager.ACTION_DOWNLOAD_COMPLETE) {
                tryInstallCompleted()
            }
        }
    }

    init {
        registerReceiver()
        tryInstallCompleted()
    }

    /// 检查更新
    fun checkUpdate(onResult: (UpdateCheckResult) -> Unit) {
        viewModelScope.launch {
            val release = updater.fetchLatestRelease()
            onResult(
                if (release == null) {
                    UpdateCheckResult.Failed(context.getString(R.string.about_check_failed))
                } else if (release.versionCode > currentVersionCode()) {
                    UpdateCheckResult.Found(release)
                } else {
                    UpdateCheckResult.Latest(versionLabel)
                }
            )
        }
    }

    /// 开始下载
    fun startUpdate(
        release: ReleaseInfo,
        onNeedPermission: () -> Unit,
        onFailed: (String) -> Unit,
    ) {
        if (!updater.canInstallApk()) {
            onNeedPermission()
            return
        }
        runCatching { updater.enqueueDownload(release.apkUrl) }
            .onFailure { onFailed(context.getString(R.string.about_download_start_failed)) }
    }

    /// 设置允许安装未知应用
    fun openInstallSettings() = updater.openInstallSettings()

    /// 下载完成且成功，唤起系统安装页
    private fun tryInstallCompleted() {
        if (!updater.canInstallApk()) return
        val file: File = updater.queryDownload() ?: return
        updater.installApk(file)
        updater.cancelPending()
    }

    /// 注册广播
    private fun registerReceiver() {
        if (receiverRegistered) return
        val filter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        ContextCompat.registerReceiver(
            context,
            downloadReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        receiverRegistered = true
    }

    override fun onCleared() {
        if (receiverRegistered) {
            runCatching { context.unregisterReceiver(downloadReceiver) }
            receiverRegistered = false
        }
        super.onCleared()
    }

    /// 已安装版本的编码
    private fun currentVersionCode(): Long =
        runCatching {
            val info = context.packageManager.getPackageInfo(context.packageName, 0)
            updater.encodeSemver(info.versionName ?: "0") ?: 0L
        }.getOrDefault(0L)
}
