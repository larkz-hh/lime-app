plugins {
    alias(libs.plugins.lime.android.feature)
}

android {
    namespace = "xyz.larkzhh.lime.feature.video"
}

dependencies {
    api(project(":core:model"))
    implementation(project(":core:ui"))
    implementation(project(":core:domain"))
    implementation(project(":core:navigation"))
    implementation(project(":core:common"))
    implementation(project(":core:data"))
    implementation(project(":core:theme"))
    implementation(project(":feature:detail"))
    implementation(project(":feature:profile"))
    implementation(project(":feature:publish"))
    implementation(project(":feature:qrscan"))
    implementation(project(":feature:translate"))
    implementation(libs.compose.danmaku)
    implementation(libs.lottie.compose)
    implementation(libs.mmkv)
    implementation(libs.swipe.back)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.session)
    implementation(libs.androidx.paging.compose)
    implementation(libs.accompanist.permissions)
}
