plugins {
    alias(libs.plugins.lime.android.library)
    alias(libs.plugins.lime.android.hilt)
}

android {
    namespace = "xyz.larkzhh.lime.core.data"
}

dependencies {
    api(project(":core:domain"))
    api(project(":core:model"))
    implementation(project(":core:common"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.paging.compose)
    implementation(libs.androidx.room.runtime)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.room.paging)
    implementation(libs.androidx.media3.exoplayer)

    implementation(libs.retrofit2)
    implementation(libs.retrofit2.converter.gson)
    implementation(libs.okhttp3)
    implementation(libs.okhttp3.logging.interceptor)
    implementation(libs.okio)
    implementation(libs.gson)

    implementation(libs.mmkv)
    implementation(libs.mlkit.translate)
    implementation(libs.vosk.android)
    implementation(libs.tencent.imsdk.plus)
    implementation(libs.shortcut.badger)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.play.services)

    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
}
