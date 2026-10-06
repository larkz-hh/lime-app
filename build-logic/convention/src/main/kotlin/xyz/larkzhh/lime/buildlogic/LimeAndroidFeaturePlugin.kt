package xyz.larkzhh.lime.buildlogic

import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType

class LimeAndroidFeaturePlugin : Plugin<Project> {
    override fun apply(target: Project): Unit = with(target) {
        pluginManager.apply("lime.android.library")
        pluginManager.apply("lime.android.hilt")
        pluginManager.apply("org.jetbrains.kotlin.plugin.compose")

        extensions.configure<LibraryExtension> {
            buildFeatures {
                compose = true
            }
        }

        val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

        dependencies {
            add("implementation", platform(libs.findLibrary("androidx-compose-bom").get()))
            add("implementation", project(":core:designsystem"))
            add("implementation", libs.findLibrary("androidx-compose-ui").get())
            add("implementation", libs.findLibrary("androidx-compose-ui-graphics").get())
            add("implementation", libs.findLibrary("androidx-compose-material3").get())
            add("implementation", libs.findLibrary("androidx-compose-material-icons-extended").get())
            add("implementation", libs.findLibrary("androidx-compose-ui-tooling-preview").get())
            add("implementation", libs.findLibrary("androidx-lifecycle-viewmodel-compose").get())
            add("implementation", libs.findLibrary("androidx-hilt-navigation-compose").get())
            add("implementation", libs.findLibrary("kotlinx-coroutines-core").get())
            add("implementation", libs.findLibrary("coil-compose").get())
            add("debugImplementation", libs.findLibrary("androidx-compose-ui-tooling").get())
        }
    }
}
