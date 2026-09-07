package xyz.larkzhh.baselineprofile

import android.os.SystemClock
import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 生成 Baseline Profile
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class BaselineProfileGenerator {

    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun generate() {
        rule.collect(
            packageName = InstrumentationRegistry.getArguments().getString("targetAppId")
                ?: throw Exception("targetAppId not passed as instrumentation runner arg"),
            includeInStartupProfile = true,
        ) {
            pressHome() //冷启动到首页首屏
            startActivityAndWait()
            device.waitForIdle()
            SystemClock.sleep(1500)// 等待首页 Paging 数据,面图加载
            device.waitForIdle()

            // 浏览首页瀑布流
            repeat(4) {
                device.swipe(500, 1600, 500, 300, 120)
                device.waitForIdle()
                SystemClock.sleep(350)
            }

            // 点击进入内容
            device.click(500, 900)
            device.waitForIdle()
            SystemClock.sleep(800)
            device.pressBack()
            device.waitForIdle()

            // 回桌面
            device.pressHome()
        }
    }
}