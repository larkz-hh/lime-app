plugins {
    alias(libs.plugins.lime.android.feature)
}

android {
    namespace = "xyz.larkzhh.lime.feature.translate"
}

dependencies {
    api(project(":core:model"))
    implementation(project(":core:ui"))
    implementation(project(":core:domain"))
    implementation(project(":core:navigation"))
    implementation(project(":core:common"))
    implementation(project(":core:data"))
    implementation(project(":core:work"))
    implementation(libs.mlkit.translate)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.androidx.media3.exoplayer)
}
