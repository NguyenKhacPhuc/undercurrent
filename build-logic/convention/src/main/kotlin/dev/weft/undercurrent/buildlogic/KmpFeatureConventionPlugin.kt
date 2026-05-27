package dev.weft.undercurrent.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * Composite plugin for `:feature:*` modules:
 *   - KMP library targets (Android + iOS)
 *   - Compose Multiplatform UI + Material3
 *   - Koin DI
 *   - Standard dependencies on core / shared modules every feature uses
 *
 * Each feature only adds its own *additional* dependencies in its
 * `build.gradle.kts` (e.g. SQLDelight if the feature persists state,
 * Coil if it loads images).
 *
 * NOTE: feature modules MUST NOT depend on Weft directly. Weft is
 * Android-only and feature modules need to compile against iOS too.
 * Weft access goes through `:data:weft` (Android-only data layer)
 * which exposes a KMP-friendly facade.
 */
class KmpFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project): Unit = with(target) {
        apply(plugin = "undercurrent.kmp.library")
        apply(plugin = "undercurrent.kmp.library.compose")
        apply(plugin = "undercurrent.kmp.koin")

        val kotlin = extensions.getByType<KotlinMultiplatformExtension>()

        // Convention: every feature gets these baseline deps.
        // Individual features add their own data/integrations deps.
        kotlin.sourceSets.named("commonMain") {
            dependencies {
                implementation(project(":core:model"))
                implementation(project(":core:ui"))
                implementation(project(":core:design-system"))
                implementation(project(":core:navigation"))
                implementation(project(":core:resources"))
            }
        }
    }
}
