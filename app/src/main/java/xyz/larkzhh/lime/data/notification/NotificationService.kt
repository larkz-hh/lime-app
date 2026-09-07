package xyz.larkzhh.lime.data.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import xyz.larkzhh.lime.R
import xyz.larkzhh.lime.data.local.TokenStorage
import xyz.larkzhh.lime.domain.repository.ImRepository
import xyz.larkzhh.lime.domain.repository.NotificationRepository
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

/**
 * 通知保活前台服务
 */
@AndroidEntryPoint
class NotificationService : Service() {

    @Inject
    lateinit var notificationCenter: NotificationCenter

    @Inject
    lateinit var tokenStorage: TokenStorage

    @Inject
    lateinit var imRepository: ImRepository

    @Inject
    lateinit var notificationRepository: NotificationRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val loginMutex = Mutex()
    private var heartbeatJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startAsForeground()
        if (!tokenStorage.isLoggedIn()) {
            stopSelf()
            return START_NOT_STICKY
        }
        scope.launch { ensureOnline() }
        startHeartbeat()// 周期心跳
        return START_STICKY
    }

    /// 确保 IM 已登录、SSE 已订阅、通知监听启动
    private suspend fun ensureOnline() {
        notificationCenter.ensureStarted()
        notificationRepository.startSse()
        loginMutex.withLock { imRepository.ensureImLogin() }
    }

    /// 心跳只自愈连接
    private suspend fun ensureConnected() {
        notificationRepository.startSse()
        loginMutex.withLock { imRepository.ensureImLogin() }
    }

    /// 周期心跳
    private fun startHeartbeat() {
        if (heartbeatJob?.isActive == true) return
        heartbeatJob = scope.launch {
            while (isActive) {
                delay(HEARTBEAT_INTERVAL_MS.milliseconds)
                if (!tokenStorage.isLoggedIn()) {
                    stopSelf()
                    break
                }
                runCatching { ensureConnected() }
            }
        }
    }

    /// 常驻前台通知
    private fun startAsForeground() {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "消息服务",
                NotificationManager.IMPORTANCE_MIN,
            ).apply {
                description = "保持消息接收连接，防止漏收通知"
                setShowBadge(false)
            },
        )
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_notification)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.notification_service_running))
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (_: Exception) {
            try {
                startForeground(NOTIFICATION_ID, notification)
            } catch (_: Exception) {
                stopSelf()
            }
        }
    }

    override fun onDestroy() {
        heartbeatJob?.cancel()
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        const val CHANNEL_ID = "lime_message_service"
        const val NOTIFICATION_ID = 3001
        private const val HEARTBEAT_INTERVAL_MS = 90_000L

        /// 启动保活服务
        fun start(context: Context) {
            try {
                context.startForegroundService(Intent(context, NotificationService::class.java))
            } catch (_: Exception) {
            }
        }

        /// 停止保活服务
        fun stop(context: Context) {
            try {
                context.stopService(Intent(context, NotificationService::class.java))
            } catch (_: Exception) {
            }
        }
    }
}
