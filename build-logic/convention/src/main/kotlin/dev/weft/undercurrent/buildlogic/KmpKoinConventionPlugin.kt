package dev.weft.undercurrent.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * Adds Koin DI dependencies to a KMP module:
 *   - `koin-core` in commonMain
 *   - `koin-compose` + `koin-compose-viewmodel` for Compose-friendly DI
 *   - `koin-android` only in androidMain (Application + lifecycle)
 *
 * Apply this together with `undercurrent.kmp.library` (or via
 * `undercurrent.kmp.feature` which composes both).
 */
class KmpKoinConventionPlugin : Plugin<Project> {
    override fun apply(target: Project): Unit = with(target) {
        val kotlin = extensions.getByType<KotlinMultiplatformExtension>()

        kotlin.sourceSets.named("commonMain") {
            dependencies {
                implementation(libs.findLibrary("koin-core").get())
                implementation(libs.findLibrary("koin-compose").get())
                implementation(libs.findLibrary("koin-compose-viewmodel").get())
            }
        }

        kotlin.sourceSets.named("androidMain") {
            dependencies {
                implementation(libs.findLibrary("koin-android").get())
                implementation(libs.findLibrary("koin-androidx-compose").get())
            }
        }
    }
}
