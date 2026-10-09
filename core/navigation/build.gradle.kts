plugins {
    alias(libs.plugins.lime.android.library)
}

android {
    namespace = "xyz.larkzhh.lime.core.navigation"
}

dependencies {
    api(project(":core:model"))
    api(libs.androidx.navigation.compose)
    api(libs.swipe.back)
}
