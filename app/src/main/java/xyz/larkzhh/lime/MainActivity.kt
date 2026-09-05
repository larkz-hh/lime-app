package xyz.larkzhh.lime

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.media3.common.util.UnstableApi
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import xyz.larkzhh.lime.data.notify.NotificationCenter
import javax.inject.Inject
import xyz.larkzhh.lime.navigation.AppNavGraph
import xyz.larkzhh.lime.navigation.ShortcutActions
import xyz.larkzhh.lime.ui.theme.LimeTheme
import xyz.larkzhh.lime.ui.video.player.VideoPlayerManager
import xyz.larkzhh.lime.ui.widget.WidgetHotCache
import xyz.larkzhh.lime.ui.widget.WidgetHotRefresher
import xyz.larkzhh.lime.util.NetworkMonitor
import xyz.larkzhh.lime.util.showToast
import kotlin.time.Duration.Companion.milliseconds

@UnstableApi
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var playerManager: VideoPlayerManager

    @Inject
    lateinit var networkMonitor: NetworkMonitor

    @Inject
    lateinit var notificationCenter: NotificationCenter

    /// 快捷入口
    private val shortcutAction = mutableStateOf<String?>(null)
    /// 快捷入口携带关键词
    private val shortcutKeyword = mutableStateOf<String?>(null)
    /// IM 快捷入口
    private val shortcutConversationId = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        resolveShortcut(intent)
        // 启动系统通知中心
        notificationCenter.ensureStarted()
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                var offlineToastJob: Job? = null
                networkMonitor.isOnline.collect { online ->
                    if (online) {
                        offlineToastJob?.cancel()
                        offlineToastJob = null
                    } else if (offlineToastJob == null) {
                        "网络开小差了，请检查网络连接".showToast(applicationContext)
                        offlineToastJob = launch {
                            var waitMs = 30_000L
                            while (true) {
                                delay(waitMs.milliseconds)
                                "网络开小差了，请检查网络连接".showToast(applicationContext)
                                waitMs = (waitMs * 2).coerceAtMost(4 * 60_000L)
                            }
                        }
                    }
                }
            }
        }
        // 进入 App 刷新桌面小组件热搜
        lifecycleScope.launch {
            if (WidgetHotCache.shouldRefresh()) {
                WidgetHotRefresher.refresh(this@MainActivity)
            }
        }
        enableEdgeToEdge()
        setContent {
            LimeTheme {
                RequestNotificationPermission()
                AppNavGraph(
                    playerManager = playerManager,
                    shortcutAction = shortcutAction.value,
                    shortcutKeyword = shortcutKeyword.value,
                    shortcutConversationId = shortcutConversationId.value,
                    onShortcutHandled = {
                        shortcutAction.value = null
                        shortcutKeyword.value = null
                        shortcutConversationId.value = null
                    },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        resolveShortcut(intent)
    }

    private fun resolveShortcut(intent: Intent?) {
        val action = intent?.action
        when {
            action == ShortcutActions.SEARCH_KEYWORD -> {
                shortcutAction.value = action
                shortcutKeyword.value = intent.getStringExtra(ShortcutActions.EXTRA_KEYWORD)
                shortcutConversationId.value = null
            }

            action == ShortcutActions.OPEN_IM_CHAT -> {
                shortcutAction.value = action
                shortcutConversationId.value = intent.getStringExtra(ShortcutActions.EXTRA_CONVERSATION_ID)
                shortcutKeyword.value = null
            }

            ShortcutActions.isShortcut(action) -> {
                shortcutAction.value = action
                shortcutKeyword.value = null
                shortcutConversationId.value = null
            }

            else -> {
                shortcutAction.value = null
                shortcutKeyword.value = null
                shortcutConversationId.value = null
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun RequestNotificationPermission() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
    val state = rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS)
    LaunchedEffect(Unit) {
        if (!state.status.isGranted) state.launchPermissionRequest()
    }
}
