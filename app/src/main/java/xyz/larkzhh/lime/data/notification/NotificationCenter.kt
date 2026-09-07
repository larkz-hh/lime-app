package xyz.larkzhh.lime.data.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import me.leolin.shortcutbadger.ShortcutBadger
import androidx.media3.common.util.UnstableApi
import xyz.larkzhh.lime.ui.MainActivity
import xyz.larkzhh.lime.R
import xyz.larkzhh.lime.data.local.TokenStorage
import xyz.larkzhh.lime.data.local.UserPreferences
import xyz.larkzhh.lime.domain.repository.ImRepository
import xyz.larkzhh.lime.domain.repository.NotificationRepository
import xyz.larkzhh.lime.navigation.action.ShortcutActions
import xyz.larkzhh.lime.domain.ForceLogoutBus
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 系统通知中心：
 * - 信箱未读数
 * - IM 新消息
 * - 桌面角标
 */
@androidx.annotation.OptIn(UnstableApi::class)
@Singleton
class NotificationCenter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val tokenStorage: TokenStorage,
    private val userPreferences: UserPreferences,
    private val notificationRepository: NotificationRepository,
    private val imRepository: ImRepository,
) {

    private val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var sessionJob: Job? = null
    private var watchJob: Job? = null

    private var lastInbox = 0
    private var lastBadge = 0

    /// 会话显示名缓存
    private val convNames = mutableMapOf<String, String>()

    init {
        createChannels()
        // 被踢下线系统通知
        scope.launch {
            ForceLogoutBus.events.collect { postForcedLogout() }
        }
    }

    /// 监听登录态，登录后跟踪同步
    fun ensureStarted() {
        sessionJob?.cancel()
        sessionJob = scope.launch {
            tokenStorage.currentUserIdFlow.collect { uid ->
                if (uid == null) {
                    watchJob?.cancel()
                    watchJob = null
                    convNames.clear()
                    ShortcutBadger.removeCount(context)
                    nm.cancelAll()
                    lastInbox = 0
                    lastBadge = 0
                    NotificationService.stop(context)
                } else {
                    startWatching()
                }
            }
        }
    }

    /// 开始监听消息与通知状态
    private fun startWatching() {
        watchJob?.cancel()
        watchJob = scope.launch {
            // 信箱聚合与桌面角标
            val inboxJob = launch {
                combine(notificationRepository.totalUnread, imRepository.conversationUnreadFlow) { inbox, im ->
                    inbox to im
                }.collect { (inbox, im) ->
                    sync(inbox, im)
                }
            }
            // IM 会话显示名缓存、已读清除该会话通知
            val convJob = launch {
                imRepository.conversationChanges.collect { conversations ->
                    conversations.forEach { conv ->
                        convNames[conv.conversationId] = conv.showName.ifBlank { conv.conversationId }
                        if (conv.unreadCount == 0) {
                            nm.cancel(convNotificationId(conv.conversationId))
                        }
                    }
                }
            }
            // IM 新消息逐条通知
            val msgJob = launch {
                imRepository.newMessages.collect { message ->
                    if (message.isSelf || message.isRevoked) return@collect
                    if (message.text == null && message.imagePath == null && message.imageUrl == null) {
                        return@collect
                    }
                    if (!userPreferences.getBoolean(UserPreferences.Keys.NOTIFY_ENABLED, true)) return@collect
                    val conversationId = message.groupId?.let { "group_$it" } ?: "c2c_${message.senderId}"
                    postImMessage(conversationId, message.text ?: "[图片]")
                }
            }
            // 会话列表首次加载建立显示名缓存
            launch {
                runCatching { imRepository.getConversations() }.getOrNull()?.forEach { conv ->
                    convNames[conv.conversationId] = conv.showName.ifBlank { conv.conversationId }
                }
            }
            inboxJob.join()
            convJob.join()
            msgJob.join()
        }
    }

    /// 偏好变化
    fun onNotifyPreferenceChanged(enabled: Boolean) {
        if (!enabled) {
            ShortcutBadger.removeCount(context)
            nm.cancelAll()
            lastBadge = 0
        }
    }

    /// 同步信箱未读数与 IM 未读数
    private fun sync(inbox: Int, im: Int) {
        val enabled = userPreferences.getBoolean(UserPreferences.Keys.NOTIFY_ENABLED, true)
        val combined = inbox + im

        if (!enabled) {
            lastInbox = inbox
            return
        }

        // 信箱聚合通知
        if (inbox > 0 && inbox > lastInbox) postInbox(inbox)
        if (inbox == 0) nm.cancel(INBOX_NOTIFICATION_ID)

        // 桌面角标 = 站内 + IM
        if (combined > 0 && combined != lastBadge) {
            runCatching { ShortcutBadger.applyCount(context, combined) }
        } else if (combined == 0 && lastBadge != 0) {
            runCatching { ShortcutBadger.removeCount(context) }
        }

        lastInbox = inbox
        lastBadge = combined
    }

    /// 发送信箱聚合通知
    private fun postInbox(count: Int) {
        val notification = NotificationCompat.Builder(context, CHANNEL_INTERACTIONS)
            .setContentTitle("互动消息")
            .setContentText("你有 $count 条新互动，点击查看")
            .setNumber(count)
            .setAutoCancel(true)
            .setContentIntent(clickIntent(ShortcutActions.OPEN_MESSAGE, null))
            .setSmallIcon(R.drawable.ic_stat_notification)
            .build()
        nm.notify(INBOX_NOTIFICATION_ID, notification)
    }

    /// 发送 IM 聊天消息通知
    private fun postImMessage(conversationId: String, preview: String) {
        val title = convNames[conversationId] ?: "新消息"
        val notification = NotificationCompat.Builder(context, CHANNEL_IM)
            .setContentTitle(title)
            .setContentText(preview)
            .setAutoCancel(true)
            .setContentIntent(clickIntent(ShortcutActions.OPEN_IM_CHAT, conversationId))
            .setSmallIcon(R.drawable.ic_stat_notification)
            .build()
        nm.notify(convNotificationId(conversationId), notification)
    }

    /// 发送账号异地登录系统通知
    private fun postForcedLogout() {
        val notification = NotificationCompat.Builder(context, CHANNEL_IM)
            .setContentTitle(context.getString(R.string.force_logout_title))
            .setContentText(context.getString(R.string.force_logout_message))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(clickIntent(ShortcutActions.OPEN_MESSAGE, null))
            .setSmallIcon(R.drawable.ic_stat_notification)
            .build()
        nm.notify(FORCED_LOGOUT_NOTIFICATION_ID, notification)
    }

    private fun clickIntent(action: String, conversationId: String?): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .setAction(action)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        if (conversationId != null) {
            intent.putExtra(ShortcutActions.EXTRA_CONVERSATION_ID, conversationId)
        }
        val requestCode = conversationId?.hashCode() ?: action.hashCode()
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    /// 生成系统通知 id
    private fun convNotificationId(conversationId: String): Int =
        IM_NOTIFICATION_ID_BASE + (conversationId.hashCode() and 0x7fffffff) % 100000

    /// 创建系统通知渠道
    private fun createChannels() {
        val interactions = NotificationChannel(
            CHANNEL_INTERACTIONS,
            "互动消息",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply { setShowBadge(true) }
        val im = NotificationChannel(
            CHANNEL_IM,
            "聊天消息",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply { setShowBadge(true) }
        nm.createNotificationChannel(interactions)
        nm.createNotificationChannel(im)
    }

    private companion object {
        const val CHANNEL_INTERACTIONS = "interactions"
        const val CHANNEL_IM = "im_chat"
        // 避开 PlaybackService 的 NOTIFICATION_ID(1001)，避免互动通知被播放通知顶掉
        const val INBOX_NOTIFICATION_ID = 4001
        const val FORCED_LOGOUT_NOTIFICATION_ID = 4002
        const val IM_NOTIFICATION_ID_BASE = 2000
    }
}
