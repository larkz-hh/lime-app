plugins {
    alias(libs.plugins.lime.android.feature)
}

android {
    namespace = "xyz.larkzhh.lime.feature.settings"
}

dependencies {
    api(project(":core:model"))
    implementation(project(":core:ui"))
    implementation(project(":core:theme"))
    implementation(project(":core:notification"))
    implementation(project(":core:domain"))
    implementation(project(":core:navigation"))
    implementation(project(":core:common"))
    implementation(project(":core:data"))
}
