import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.baselineprofile)
}

// ABI 裁剪开关：./gradlew assembleRelease -PslimAbi=true
val slimAbi = providers.gradleProperty("slimAbi").map { it.toBoolean() }.getOrElse(false)

// 后端地址注入
val localBuildProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) load(f.inputStream())
}
val apiBaseUrl: String =
    (localBuildProps.getProperty("BASE_URL")?.trim()?.takeIf { it.isNotBlank() }
        ?: "http://127.0.0.1:8080/") // 占位地址
        .let { if (it.endsWith("/")) it else "$it/" }

// release 正式签名
val keystoreProps = Properties().apply {
    val f = rootProject.file("keystore.properties")
    if (f.exists()) load(f.inputStream())
}

fun keystoreProp(name: String): String? =
    keystoreProps.getProperty(name)?.trim()?.takeIf { it.isNotBlank() && !it.contains("改成") }

val releaseSigningReady = listOf("storeFile", "storePassword", "keyAlias").all { keystoreProp(it) != null }

android {
    namespace = "xyz.larkzhh.lime"
    compileSdk = 37

    defaultConfig {
        applicationId = "xyz.larkzhh.lime"
        minSdk = 26
        targetSdk = 36
        versionCode = 2
        versionName = "1.0.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "API_BASE_URL", "\"$apiBaseUrl\"")

        if (slimAbi) {
            ndk { abiFilters += listOf("arm64-v8a") }
        }
    }

    signingConfigs {
        if (releaseSigningReady) {
            create("release") {
                storeFile = file(keystoreProp("storeFile")!!)
                storePassword = keystoreProp("storePassword")
                keyAlias = keystoreProp("keyAlias")
                // PKCS12：key 密码必须与 store 密码一致
                keyPassword = keystoreProp("storePassword")
            }
        }
    }

    buildTypes {
        release {
            signingConfig = if (releaseSigningReady) {
                signingConfigs.getByName("release")
            } else {
                logger.warn("keystore.properties 未配置完整，release 将使用 debug 签名")
                signingConfigs.getByName("debug")
            }
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21
        freeCompilerArgs.add("-XXLanguage:+PropertyParamAnnotationDefaultTargetMode")
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.collection)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    // Navigation
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.profileinstaller)

    // DI
    implementation(libs.hilt.android)
    "baselineProfile"(project(":baselineprofile"))
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    // Paging
    implementation(libs.androidx.paging.runtime)
    implementation(libs.androidx.paging.compose)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.room.paging)

    // Network
    implementation(libs.retrofit2)
    implementation(libs.retrofit2.converter.gson)
    implementation(libs.okhttp3)
    implementation(libs.okhttp3.logging.interceptor)
    implementation(libs.okio)

    // Image & Video Loading
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
    implementation(libs.coil.video)
//    implementation(libs.androidx.palette)

    // kv Storage
    implementation(libs.mmkv)

    // Coroutines
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)

    // ML Kit
    implementation(libs.mlkit.translate)
    implementation(libs.mlkit.barcode.scanning)

    // QRCode generate
    implementation(libs.zxing.core)

    // CameraX
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)

    // Lottie Compose
    implementation(libs.lottie.compose)

    // exyte 动画底部导航
    implementation(libs.exyte.animated.navigation.bar)

    // Media3 ExoPlayer
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.common.ktx)
    implementation(libs.androidx.media3.ui.compose)
    implementation(libs.androidx.media3.ui.compose.material3)
    implementation(libs.androidx.media3.session)

    // Permissions
    implementation(libs.accompanist.permissions)

    // WorkManager
    implementation(libs.androidx.work.runtime.ktx)

    // Image Crop
    implementation(libs.ucrop)

    // Drag-and-drop reorder
    implementation(libs.reorderable)

    // Toast
    implementation(libs.toasty)

    // Telephoto zoomable image
    implementation(libs.telephoto.zoomable.image.coil3)

    // Markdown
    implementation(libs.markdown.renderer.m3)
    implementation(libs.markdown.renderer.coil3)

    // Splash Screen
//    implementation(libs.androidx.core.splashscreen)

    // Widget
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.glance.material3)
    implementation(libs.androidx.glance.appwidget.preview)
    implementation(libs.androidx.glance.preview)

    // 腾讯云 IM
    implementation(libs.tencent.imsdk.plus)

    // 桌面角标（厂商聚合）
    implementation(libs.shortcut.badger)

    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
