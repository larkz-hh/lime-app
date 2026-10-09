// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.android.test) apply false
    alias(libs.plugins.android.built.in1.kotlin) apply false
    alias(libs.plugins.baselineprofile) apply false
    alias(libs.plugins.dependency.analysis)
}

dependencyAnalysis {
    issues {
        all {
            onUnusedDependencies { severity("warn") }
            onUsedTransitiveDependencies { severity("warn") }
            onIncorrectConfiguration { severity("warn") }
            onUnusedAnnotationProcessors { severity("warn") }
            onRedundantPlugins { severity("warn") }
        }
    }
}

subprojects {
    apply(plugin = "com.autonomousapps.dependency-analysis")
}