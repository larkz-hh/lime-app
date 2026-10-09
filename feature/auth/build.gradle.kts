plugins {
    alias(libs.plugins.lime.android.feature)
}

android {
    namespace = "xyz.larkzhh.lime.feature.auth"
}

dependencies {
    implementation(project(":core:domain"))
    implementation(project(":core:common"))
}
