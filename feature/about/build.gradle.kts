plugins {
    alias(libs.plugins.lime.android.feature)
}

android {
    namespace = "xyz.larkzhh.lime.feature.about"
}

dependencies {
    implementation(project(":core:ui"))
    implementation(project(":core:common"))
    implementation(libs.mmkv)
    implementation(libs.okhttp3)
}
