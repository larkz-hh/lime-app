import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    `kotlin-dsl`
}

group = "xyz.larkzhh.lime.buildlogic"

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
    }
}

dependencies {
    compileOnly(libs.android.gradle.plugin)
    compileOnly(libs.kotlin.gradle.plugin)
}

gradlePlugin {
    plugins {
        register("androidApplication") {
            id = "lime.android.application"
            implementationClass = "xyz.larkzhh.lime.buildlogic.LimeAndroidApplicationPlugin"
        }
        register("androidHilt") {
            id = "lime.android.hilt"
            implementationClass = "xyz.larkzhh.lime.buildlogic.LimeAndroidHiltPlugin"
        }
        register("androidLibrary") {
            id = "lime.android.library"
            implementationClass = "xyz.larkzhh.lime.buildlogic.LimeAndroidLibraryPlugin"
        }
        register("androidLibraryCompose") {
            id = "lime.android.library.compose"
            implementationClass = "xyz.larkzhh.lime.buildlogic.LimeAndroidLibraryComposePlugin"
        }
        register("kotlinLibrary") {
            id = "lime.kotlin.library"
            implementationClass = "xyz.larkzhh.lime.buildlogic.LimeKotlinLibraryPlugin"
        }
        register("androidFeature") {
            id = "lime.android.feature"
            implementationClass = "xyz.larkzhh.lime.buildlogic.LimeAndroidFeaturePlugin"
        }
        register("androidBaselineProfile") {
            id = "lime.android.baselineprofile"
            implementationClass = "xyz.larkzhh.lime.buildlogic.LimeAndroidBaselineProfilePlugin"
        }
    }
}
