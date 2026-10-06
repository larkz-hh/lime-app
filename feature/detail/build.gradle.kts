plugins {
    alias(libs.plugins.lime.android.feature)
}

android {
    namespace = "xyz.larkzhh.lime.feature.detail"
}

dependencies {
    api(project(":core:model"))
    implementation(project(":core:ui"))
    implementation(project(":core:domain"))
    implementation(project(":core:navigation"))
    implementation(project(":core:common"))
    implementation(project(":core:data"))
    implementation(project(":feature:profile"))
    implementation(project(":feature:publish"))
    implementation(project(":feature:translate"))
    implementation(project(":feature:speech"))
    implementation(libs.androidx.paging.compose)
    implementation(libs.accompanist.permissions)
}
