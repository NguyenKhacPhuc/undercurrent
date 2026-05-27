package dev.weft.undercurrent.buildlogic

import com.android.build.gradle.LibraryExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * KMP "shared app" — the surface both `:androidApp` and `:iosApp`
 * consume. Targets:
 *   - `androidTarget()` so :androidApp can include this as a library
 *   - iOS frameworks (`iosArm64`, `iosSimulatorArm64`) configured to
 *     produce a static `ComposeApp.framework` :iosApp embeds in Xcode
 *
 * Use for `:composeApp` only. Feature modules use
 * `undercurrent.kmp.feature`. The Android app shell uses
 * `undercurrent.android.application`.
 */
class KmpApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project): Unit = with(target) {
        apply(plugin = "org.jetbrains.kotlin.multiplatform")
        apply(plugin = "com.android.library")
        apply(plugin = "org.jetbrains.compose")
        apply(plugin = "org.jetbrains.kotlin.plugin.compose")

        val kotlin = extensions.getByType<KotlinMultiplatformExtension>()
        kotlin.applyDefaultHierarchyTemplate()

        kotlin.androidTarget {
            compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
        }

        listOf(
            kotlin.iosArm64(),
            kotlin.iosSimulatorArm64(),
        ).forEach { iosTarget ->
            iosTarget.binaries.framework {
                baseName = "ComposeApp"
                isStatic = true
                // Link the SQLite library that SQLDelight's native driver
                // requires on iOS. Cheap to add unconditionally.
                linkerOpts("-lsqlite3")
            }
        }

        kotlin.sourceSets.named("commonMain") {
            dependencies {
                implementation(libs.findLibrary("compose-multiplatform-runtime").get())
                implementation(libs.findLibrary("compose-multiplatform-foundation").get())
                implementation(libs.findLibrary("compose-multiplatform-material3").get())
                implementation(libs.findLibrary("compose-multiplatform-ui").get())
                implementation(libs.findLibrary("compose-multiplatform-resources").get())
                implementation(libs.findLibrary("kotlinx-coroutines-core").get())
            }
        }

        // KMP + com.android.library = need to configure LibraryExtension
        // explicitly. Without this AGP fails with "compileSdkVersion is
        // not specified".
        extensions.configure<LibraryExtension> {
            compileSdk = androidCompileSdk
            defaultConfig {
                minSdk = androidMinSdk
            }
            compileOptions {
                sourceCompatibility = JavaVersion.VERSION_17
                targetCompatibility = JavaVersion.VERSION_17
            }
        }
    }
}
