package xyz.larkzhh.lime.buildlogic

import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class LimeAndroidLibraryPlugin : Plugin<Project> {
    override fun apply(target: Project): Unit = with(target) {
        pluginManager.apply("com.android.library")
        pluginManager.apply("org.jetbrains.kotlin.android")

        extensions.configure<LibraryExtension> {
            compileSdk = LimeConfig.COMPILE_SDK

            defaultConfig {
                minSdk = LimeConfig.MIN_SDK
            }

            compileOptions {
                sourceCompatibility = LimeConfig.JAVA_VERSION
                targetCompatibility = LimeConfig.JAVA_VERSION
            }

            buildFeatures {
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
