package xyz.larkzhh.lime.data.local

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToLong

/**
 * App 缓存管理器
 *
 * - Room 数据库文件（AI 聊天、信息流、笔记详情、通知缓存）
 * - 应用 cacheDir（图片加载缓存、网络缓存、临时文件等）
 *
 * 保留：
 * - MMKV（登录态 Token、用户偏好、搜索历史等）
 * - IM SDK 自身数据
 */
@Singleton
class AppCacheManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userDatabases: UserDatabases,
) {

    // 数据库目录
    private val databaseDir: File
        get() = context.getDatabasePath("placeholder.db").parentFile ?: context.filesDir

    /// 统计当前可清理缓存字节数
    fun cacheSizeBytes(): Long =
        sizeOf(databaseDir) + sizeOf(context.cacheDir)

    /// 缓存大小文案
    fun cacheSizeLabel(): String {
        val bytes = cacheSizeBytes()
        val mb = bytes / (1024.0 * 1024.0)
        val gb = bytes / (1024.0 * 1024.0 * 1024.0)
        return if (gb >= 1.0) {
            String.format(Locale.US, "%.2fGB", gb)
        } else if (bytes >= 1024 * 1024) {
            "${mb.roundToLong()}MB"
        } else {
            "<1MB"
        }
    }

    /// 清空可恢复缓存
    fun clearCache() {
        // 关闭 Room 实例
        userDatabases.closeAll()
        deleteRecursively(databaseDir)
        deleteRecursively(context.cacheDir)
    }

    /// 递归统计文件或目录占用的总字节数
    private fun sizeOf(file: File): Long {
        if (!file.exists()) return 0L
        if (file.isFile) return file.length()
        return file.listFiles()?.sumOf { sizeOf(it) } ?: 0L
    }

    /// 递归删除文件或目录
    private fun deleteRecursively(file: File) {
        if (!file.exists()) return
        if (file.isDirectory) {
            file.listFiles()?.forEach { deleteRecursively(it) }
        }
        file.delete()
    }
}
