plugins {
    alias(libs.plugins.lime.android.feature)
}

android {
    namespace = "xyz.larkzhh.lime.feature.home"
}

dependencies {
    api(project(":core:model"))
    implementation(project(":core:ui"))
    implementation(project(":core:domain"))
    implementation(project(":core:data"))
    implementation(project(":core:navigation"))
    implementation(project(":core:common"))
    implementation(libs.androidx.paging.compose)
}
