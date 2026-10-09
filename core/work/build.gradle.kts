plugins {
    alias(libs.plugins.lime.android.library)
    alias(libs.plugins.lime.android.hilt)
}

android {
    namespace = "xyz.larkzhh.lime.core.work"
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:data"))
    implementation(project(":core:designsystem"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.work.runtime)
    implementation(libs.mlkit.translate)
    implementation(libs.mmkv)
}
