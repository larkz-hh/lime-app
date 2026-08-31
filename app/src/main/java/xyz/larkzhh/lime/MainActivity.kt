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
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.util.UnstableApi
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject
import xyz.larkzhh.lime.navigation.AppNavGraph
import xyz.larkzhh.lime.navigation.ShortcutActions
import xyz.larkzhh.lime.ui.theme.LimeTheme
import xyz.larkzhh.lime.ui.video.player.VideoPlayerManager
import xyz.larkzhh.lime.ui.widget.WidgetHotCache
import xyz.larkzhh.lime.ui.widget.WidgetHotRefresher

@UnstableApi
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var playerManager: VideoPlayerManager

    /// 快捷入口
    private val shortcutAction = mutableStateOf<String?>(null)
    /// 快捷入口携带关键词
    private val shortcutKeyword = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        resolveShortcut(intent)
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
                    onShortcutHandled = {
                        shortcutAction.value = null
                        shortcutKeyword.value = null
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
            }

            ShortcutActions.isShortcut(action) -> {
                shortcutAction.value = action
                shortcutKeyword.value = null
            }

            else -> {
                shortcutAction.value = null
                shortcutKeyword.value = null
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
