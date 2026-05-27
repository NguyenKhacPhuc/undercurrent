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
 * Baseline KMP library — targets Android + iOS, no Compose, no Koin.
 *
 * Use for pure-Kotlin modules: `:core:model`, `:core:ext`, `:core:domain`,
 * `:shared`. For anything Compose-driven add the
 * `undercurrent.kmp.library.compose` plugin on top. For DI-using
 * modules add `undercurrent.kmp.koin`.
 *
 * Targets: `android()` + `iosArm64()` + `iosSimulatorArm64()`. We
 * deliberately don't target `iosX64()` — Apple Silicon Macs only, no
 * Rosetta path. Add it back when CI needs Intel-Mac coverage.
 */
class KmpLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project): Unit = with(target) {
        apply(plugin = "org.jetbrains.kotlin.multiplatform")
        apply(plugin = "com.android.library")

        val kotlin = extensions.getByType<KotlinMultiplatformExtension>()
        kotlin.applyDefaultHierarchyTemplate()

        @Suppress("UNUSED_VARIABLE")
        kotlin.androidTarget {
            compilerOptions {
                jvmTarget.set(JvmTarget.JVM_17)
            }
        }

        kotlin.iosArm64()
        kotlin.iosSimulatorArm64()

        kotlin.sourceSets.named("commonMain") {
            dependencies {
                implementation(libs.findLibrary("kotlinx-coroutines-core").get())
                implementation(libs.findLibrary("kotlinx-serialization-json").get())
            }
        }

        kotlin.sourceSets.named("commonTest") {
            dependencies {
                implementation(libs.findLibrary("kotlin-test").get())
                implementation(libs.findLibrary("kotlinx-coroutines-test").get())
            }
        }

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
