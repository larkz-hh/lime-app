plugins {
    alias(libs.plugins.lime.android.library.compose)
    alias(libs.plugins.lime.android.hilt)
}

android {
    namespace = "xyz.larkzhh.lime.core.ui"
}

dependencies {
    api(project(":core:model"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:common"))
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.paging.compose)
    implementation(libs.coil.compose)
    implementation(libs.telephoto.zoomable.image.coil3)
    implementation(libs.lottie.compose)

    debugImplementation(libs.androidx.compose.ui.tooling)
}
