plugins {
    alias(libs.plugins.lime.android.library.compose)
    alias(libs.plugins.lime.android.hilt)
}

android {
    namespace = "xyz.larkzhh.lime.core.theme"
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:data"))
    implementation(project(":core:designsystem"))
    implementation(libs.mmkv)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.foundation)
}
