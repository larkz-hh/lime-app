plugins {
    alias(libs.plugins.lime.android.feature)
}

android {
    namespace = "xyz.larkzhh.lime.feature.qrscan"
}

dependencies {
    api(project(":core:model"))
    implementation(project(":core:ui"))
    implementation(project(":core:domain"))
    implementation(project(":core:navigation"))
    implementation(project(":core:common"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.mlkit.barcode.scanning)
    implementation(libs.accompanist.permissions)
}
