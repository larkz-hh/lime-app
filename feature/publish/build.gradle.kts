plugins {
    alias(libs.plugins.lime.android.feature)
}

android {
    namespace = "xyz.larkzhh.lime.feature.publish"
}

dependencies {
    api(project(":core:model"))
    implementation(project(":core:ui"))
    implementation(project(":core:domain"))
    implementation(project(":core:navigation"))
    implementation(project(":core:common"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.coil.video)
    implementation(libs.telephoto.zoomable.image.coil3)
    implementation(libs.reorderable)
    implementation(libs.accompanist.permissions)
}
