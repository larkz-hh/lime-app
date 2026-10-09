package xyz.larkzhh.lime.ui.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.milliseconds

object WidgetHotRefresher {

    /// 拉取并刷新
    suspend fun refresh(context: Context): Boolean {
        val fresh = withTimeoutOrNull(WidgetHotCache.FETCH_TIMEOUT_MS.milliseconds) {
            runCatching {
                val entryPoint =
                    EntryPointAccessors.fromApplication(context, WidgetEntryPoint::class.java)
                entryPoint.searchRepository()
                    .getHotSearches(WidgetHotCache.MAX_HOT_COUNT)
                    .getOrDefault(emptyList())
            }.getOrNull()
        }
        if (fresh == null) return false

        WidgetHotCache.write(fresh)
        runCatching { SearchWidget().updateAll(context) }
        return true
    }
}
