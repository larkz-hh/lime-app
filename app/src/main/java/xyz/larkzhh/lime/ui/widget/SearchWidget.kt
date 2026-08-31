package xyz.larkzhh.lime.ui.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.color.ColorProvider
import androidx.glance.preview.ExperimentalGlancePreviewApi
import androidx.glance.preview.Preview
import androidx.media3.common.util.UnstableApi
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import xyz.larkzhh.lime.MainActivity
import xyz.larkzhh.lime.R
import xyz.larkzhh.lime.data.network.model.HotSearchItem
import xyz.larkzhh.lime.navigation.ShortcutActions
import xyz.larkzhh.lime.work.WidgetHotRefreshWorker

/// 搜索栏小组件
class SearchWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Exact

    @UnstableApi
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        WidgetHotRefreshWorker.ensureScheduled(context)
        // 读取缓存
        val cached = WidgetHotCache.read()

        coroutineScope {
            launch {
                if (WidgetHotCache.shouldRefresh()) {
                    WidgetHotRefresher.refresh(context)
                }
            }
            provideContent {
                SearchWidgetContent(hotSearches = cached)
            }
        }
    }
}

@UnstableApi
@Composable
private fun SearchWidgetContent(
    hotSearches: List<HotSearchItem>,
    showCount: Int? = null,
) {
    val context = LocalContext.current
    val effectiveCount = showCount ?: run {
        val size = LocalSize.current
        when {
            size.height >= 300.dp -> WidgetHotCache.MAX_HOT_COUNT
            size.height >= 190.dp -> 4
            else -> 3
        }
    }

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
            .padding(16.dp)
            .clickable(
                onClick = actionStartActivity(
                    Intent(context, MainActivity::class.java).apply {
                        action = ShortcutActions.SEARCH
                    }
                )
            ),
    ) {
        // 搜索框
        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 14.dp, vertical = 11.dp)
                .clickable(
                    onClick = actionStartActivity(
                        Intent(context, MainActivity::class.java).apply {
                            action = ShortcutActions.SEARCH
                        }
                    )
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                provider = ImageProvider(R.drawable.ic_shortcut_search),
                contentDescription = "搜索",
                modifier = GlanceModifier.width(16.dp).height(16.dp),
            )
            Spacer(GlanceModifier.width(8.dp))
            Text(
                text = "搜索笔记",
                style = TextStyle(
                    color = ColorProvider(day = Color(0xFF999999), night = Color(0xFF999999)),
                    fontSize = 14.sp,
                ),
            )
        }

        Spacer(GlanceModifier.height(12.dp))
        Text(
            text = "今日热搜",
            style = TextStyle(
                color = ColorProvider(day = Color(0xFF111111), night = Color(0xFF111111)),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
            ),
        )
        Spacer(GlanceModifier.height(6.dp))

        if (hotSearches.isEmpty()) {
            Text(
                text = "暂无热搜",
                style = TextStyle(
                    color = ColorProvider(day = Color(0xFF999999), night = Color(0xFF999999)),
                    fontSize = 12.sp,
                ),
            )
        } else {
            hotSearches.take(effectiveCount).forEachIndexed { index, item ->
                HotSearchRow(index = index, item = item)
            }
        }
    }
}

@UnstableApi
@Composable
private fun HotSearchRow(
    index: Int,
    item: HotSearchItem,
) {
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
            .clickable(
                onClick = actionRunCallback<StartSearchActionCallback>(
                    actionParametersOf(
                        StartSearchActionCallback.KEYWORD to item.keyword
                    )
                )
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "${index + 1}",
            style = TextStyle(
                color = ColorProvider(
                    day = if (index < 3) Color(0xFFFF6B35) else Color(0xFF999999),
                    night = if (index < 3) Color(0xFFFF6B35) else Color(0xFF999999),
                ),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
            ),
            modifier = GlanceModifier.width(20.dp),
        )
        Spacer(GlanceModifier.width(6.dp))
        Text(
            text = item.keyword,
            style = TextStyle(
                color = ColorProvider(day = Color(0xFF333333), night = Color(0xFF333333)),
                fontSize = 13.sp,
            ),
            maxLines = 1,
            modifier = GlanceModifier.defaultWeight(),
        )
        Spacer(GlanceModifier.width(8.dp))
        Text(
            text = "${item.count}",
            style = TextStyle(
                color = ColorProvider(day = Color(0xFF999999), night = Color(0xFF999999)),
                fontSize = 11.sp,
            ),
        )
    }
}


@OptIn(ExperimentalGlancePreviewApi::class)
@UnstableApi
@Preview(widthDp = 250, heightDp = 250)
@Composable
fun SearchWidgetPreview() {
    SearchWidgetContent(
        hotSearches = previewHotSearches(),
        showCount = 3,
    )
}

private fun previewHotSearches(): List<HotSearchItem> = listOf(
    HotSearchItem("关注三院科学喵", 128903),
    HotSearchItem("安卓桌面小组件怎么做", 88432),
    HotSearchItem("Jetpack Glance 入门", 114514),
)
