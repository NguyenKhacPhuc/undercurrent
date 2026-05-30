package dev.weft.undercurrent.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * Layered on top of `undercurrent.kmp.library` — adds Compose
 * Multiplatform. The CMP Gradle plugin brings the runtime + UI +
 * foundation + material3 dependencies; we explicitly add `components`
 * and `resources` because not every CMP target pulls them
 * transitively.
 *
 * Apply after `undercurrent.kmp.library` (the `.kmp.feature` plugin
 * does this composition automatically).
 */
class KmpLibraryComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project): Unit = with(target) {
        apply(plugin = "org.jetbrains.compose")
        apply(plugin = "org.jetbrains.kotlin.plugin.compose")

        val kotlin = extensions.getByType<KotlinMultiplatformExtension>()

        kotlin.sourceSets.named("commonMain") {
            dependencies {
                implementation(libs.findLibrary("compose-multiplatform-runtime").get())
                implementation(libs.findLibrary("compose-multiplatform-foundation").get())
                implementation(libs.findLibrary("compose-multiplatform-material3").get())
                implementation(libs.findLibrary("compose-multiplatform-ui").get())
                implementation(libs.findLibrary("compose-multiplatform-resources").get())
                // @Preview annotation usable from commonMain. The CMP
                // preview panel (IntelliJ + Android Studio with the
                // CMP plugin) discovers @Preview functions wherever
                // they live; the AndroidX `androidx.compose.ui:
                // ui-tooling-preview` artifact is the Android-only
                // equivalent — we standardise on the CMP one for
                // KMP-clean preview functions.
                implementation(libs.findLibrary("compose-multiplatform-ui-tooling-preview").get())
            }
        }
    }
}
