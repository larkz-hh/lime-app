package xyz.larkzhh.lime.buildlogic

import org.gradle.api.JavaVersion

/** 项目 Android 构建参数。 */
internal object LimeConfig {
    const val COMPILE_SDK = 37
    const val MIN_SDK = 26
    const val TARGET_SDK = 36
    const val INSTRUMENTATION_RUNNER = "androidx.test.runner.AndroidJUnitRunner"

    val JAVA_VERSION: JavaVersion = JavaVersion.VERSION_21
}
