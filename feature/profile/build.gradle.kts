plugins {
    alias(libs.plugins.lime.android.feature)
}

android {
    namespace = "xyz.larkzhh.lime.feature.profile"
}

dependencies {
    implementation(libs.androidx.compose.ui.tooling.preview)
    api(project(":core:model"))
    implementation(project(":core:ui"))
    implementation(project(":core:domain"))
    implementation(project(":core:navigation"))
    implementation(project(":core:common"))
    implementation(project(":feature:im"))
    implementation(libs.androidx.paging.compose)
    implementation(libs.ucrop)
    implementation(libs.gson)
}
