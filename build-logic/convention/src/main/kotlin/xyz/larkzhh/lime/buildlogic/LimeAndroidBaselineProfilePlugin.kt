package xyz.larkzhh.lime.buildlogic

import com.android.build.api.dsl.TestExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class LimeAndroidBaselineProfilePlugin : Plugin<Project> {
    override fun apply(target: Project): Unit = with(target) {
        pluginManager.apply("com.android.test")
        pluginManager.apply("org.jetbrains.kotlin.android")
        pluginManager.apply("androidx.baselineprofile")

        extensions.configure<TestExtension> {
            compileSdk = LimeConfig.COMPILE_SDK

            defaultConfig {
                minSdk = LimeConfig.MIN_SDK
                targetSdk = LimeConfig.TARGET_SDK
                testInstrumentationRunner = LimeConfig.INSTRUMENTATION_RUNNER
                testInstrumentationRunnerArguments["androidx.benchmark.suppressErrors"] = "EMULATOR"
            }

            compileOptions {
                sourceCompatibility = LimeConfig.JAVA_VERSION
                targetCompatibility = LimeConfig.JAVA_VERSION
            }
        }

        configureKotlinAndroid()
    }
}
