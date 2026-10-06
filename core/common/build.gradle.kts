plugins {
    alias(libs.plugins.lime.android.library)
    alias(libs.plugins.lime.android.hilt)
}

android {
    namespace = "xyz.larkzhh.lime.core.common"
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.collection)
    implementation(libs.androidx.exifinterface)
    implementation(libs.gson)
    implementation(libs.zxing.core)
    implementation(libs.mmkv)
    implementation(libs.toasty)
    implementation(libs.coil.compose)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.play.services)

    testImplementation(libs.junit)
}
