package xyz.larkzhh.lime.ui.about

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Environment
import android.provider.Settings
import androidx.core.content.FileProvider
import com.tencent.mmkv.MMKV
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import androidx.core.net.toUri
import xyz.larkzhh.lime.R

/// GitHub Release 信息
data class ReleaseInfo(
    val versionCode: Long,
    val versionName: String,
    val notes: String,
    val apkUrl: String,
)

/**
 * GitHub 版本检查与 APK 下载安装
 */
@Singleton
class AppUpdater @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    private val client = OkHttpClient()

    /// 请求 GitHub 最新 Release 并解析
    suspend fun fetchLatestRelease(): ReleaseInfo? = withContext(Dispatchers.IO) {
        val repo = AboutConfig.REPO.ifBlank { return@withContext null }
        val request = Request.Builder()
            .url("https://api.github.com/repos/$repo/releases/latest")
            .header("Accept", "application/vnd.github+json")
            .build()
        runCatching {
            client.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) return@use null
                val json = JSONObject(resp.body?.string().orEmpty())
                val tag = json.optString("tag_name")
                val notes = json.optString("body")
                val assets = json.optJSONArray("assets")
                val versionCode = encodeSemver(tag) ?: return@use null
                var apkUrl: String? = null
                var fallbackUrl: String? = null
                if (assets != null) {
                    for (i in 0 until assets.length()) {
                        val a = assets.optJSONObject(i) ?: continue
                        val name = a.optString("name")
                        if (!name.endsWith(".apk", ignoreCase = true)) continue
                        val url = a.optString("browser_download_url").ifBlank { a.optString("url") }
                        val lower = name.lowercase()
                        val isDedicatedAbi =
                            lower.contains("arm64") || lower.contains("armv7") || lower.contains("x86")
                        if (lower.contains("universal")) {
                            apkUrl = url
                        } else if (!isDedicatedAbi && fallbackUrl == null) {
                            fallbackUrl = url
                        }
                    }
                }
                val url = apkUrl ?: fallbackUrl ?: return@use null
                ReleaseInfo(versionCode, tag, notes, url)
            }
        }.getOrNull()
    }

    /// 语义化版本号编码
    fun encodeSemver(name: String): Long? {
        val nums = Regex("\\d+").findAll(name).map { it.value.toLong() }.toList()
        if (nums.isEmpty()) return null
        val major = nums.getOrElse(0) { 0L }
        val minor = nums.getOrElse(1) { 0L }
        val patch = nums.getOrElse(2) { 0L }
        return major * 1_000_000 + minor * 1_000 + patch
    }

    /// APK 保存路径
    private fun targetFile(): File {
        val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            ?: context.filesDir
        return File(dir, APK_FILE_NAME)
    }

    /// 加入系统下载队列
    fun enqueueDownload(url: String): Long {
        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        cancelPending() // 清理旧任务
        targetFile().delete()
        val request = DownloadManager.Request(url.toUri())
            .setTitle(context.getString(R.string.updater_notify_title))
            .setDescription(context.getString(R.string.updater_notify_desc))
            .setMimeType("application/vnd.android.package-archive")
            .setDestinationUri(Uri.fromFile(targetFile()))
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        val id = dm.enqueue(request)
        MMKV.defaultMMKV().encode(KEY_DOWNLOAD_ID, id)
        return id
    }

    /// 查询下载结果
    fun queryDownload(): File? {
        val id = MMKV.defaultMMKV().decodeLong(KEY_DOWNLOAD_ID, -1L)
        if (id <= 0) return null
        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val file = targetFile()
        val cursor: Cursor? = dm.query(DownloadManager.Query().setFilterById(id))
        return try {
            cursor?.use { c ->
                if (c.moveToFirst()) {
                    val status = c.getInt(c.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                    if (status == DownloadManager.STATUS_SUCCESSFUL && file.exists() && file.length() > 0) {
                        file
                    } else {
                        null
                    }
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            null
        }
    }

    /// 取消进行中的下载任务
    fun cancelPending() {
        val id = MMKV.defaultMMKV().decodeLong(KEY_DOWNLOAD_ID, -1L)
        if (id <= 0) return
        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        runCatching { dm.remove(id) }
        MMKV.defaultMMKV().removeValueForKey(KEY_DOWNLOAD_ID)
    }

    /// 判断能否调起系统安装页
    fun canInstallApk(): Boolean = context.packageManager.canRequestPackageInstalls()

    /// 跳转允许安装未知应用页
    fun openInstallSettings() {
        val intent = Intent(
            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
            "package:${context.packageName}".toUri(),
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
    }

    /// 调起系统安装页
    fun installApk(file: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, "application/vnd.android.package-archive")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
    }

    private companion object {
        const val APK_FILE_NAME = "lime_update.apk"
        const val KEY_DOWNLOAD_ID = "app_update_download_id"
    }
}
