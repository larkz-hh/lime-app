plugins {
    alias(libs.plugins.lime.android.library.compose)
}

android {
    namespace = "xyz.larkzhh.lime.core.designsystem"
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.ui.tooling.preview)

    implementation(libs.swipe.back)
    implementation(libs.coil.compose)
    implementation(libs.markdown.renderer.m3)
    implementation(libs.markdown.renderer.coil3)

    debugImplementation(libs.androidx.compose.ui.tooling)
}
