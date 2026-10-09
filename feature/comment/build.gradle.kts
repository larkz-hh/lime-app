plugins {
    alias(libs.plugins.lime.android.feature)
}

android {
    namespace = "xyz.larkzhh.lime.feature.comment"
}

dependencies {
    api(project(":core:model"))
    implementation(project(":core:ui"))
    implementation(project(":core:domain"))
    implementation(project(":core:navigation"))
    implementation(project(":core:common"))
    implementation(project(":core:data"))
    implementation(project(":feature:publish"))
    implementation(project(":feature:speech"))
    implementation(libs.accompanist.permissions)
}
