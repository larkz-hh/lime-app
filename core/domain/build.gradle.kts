plugins {
    alias(libs.plugins.lime.android.library)
}

android {
    namespace = "xyz.larkzhh.lime.core.domain"
}

dependencies {
    api(project(":core:model"))
    api(libs.androidx.paging.common)
    api(libs.kotlinx.coroutines.core)
}
