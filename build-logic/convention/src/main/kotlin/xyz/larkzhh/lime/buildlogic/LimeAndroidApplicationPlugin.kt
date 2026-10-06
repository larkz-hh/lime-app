package xyz.larkzhh.lime.buildlogic

import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class LimeAndroidApplicationPlugin : Plugin<Project> {
    override fun apply(target: Project): Unit = with(target) {
        pluginManager.apply("com.android.application")
        pluginManager.apply("org.jetbrains.kotlin.android")
        pluginManager.apply("org.jetbrains.kotlin.plugin.compose")

        extensions.configure<ApplicationExtension> {
            compileSdk = LimeConfig.COMPILE_SDK

            defaultConfig {
                minSdk = LimeConfig.MIN_SDK
                targetSdk = LimeConfig.TARGET_SDK
                testInstrumentationRunner = LimeConfig.INSTRUMENTATION_RUNNER
            }

            compileOptions {
                sourceCompatibility = LimeConfig.JAVA_VERSION
                targetCompatibility = LimeConfig.JAVA_VERSION
            }

            buildFeatures {
                compose = true
                buildConfig = true
            }
            
            testOptions {
                unitTests {
                    isReturnDefaultValues = true
                }
            }
        }

        configureKotlinAndroid()
    }
}
