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
        // Mokkery — KMP mocking via Kotlin compiler plugin. Applied to
        // every KMP library so commonTest can call `mock<T>()` directly
        // without per-module opt-in. The compiler plugin no-ops modules
        // that don't use mocks; cost is negligible.
        apply(plugin = "dev.mokkery")

        val kotlin = extensions.getByType<KotlinMultiplatformExtension>()
        kotlin.applyDefaultHierarchyTemplate()

        @Suppress("UNUSED_VARIABLE")
        kotlin.androidTarget {
            compilerOptions {
                jvmTarget.set(JvmTarget.JVM_17)
            }
        }

        // expect/actual classes are still flagged "in Beta" by Kotlin
        // 2.x even though they're widely used. -Xexpect-actual-classes
        // opts in across every KMP module so we don't get the warning
        // for every DatabaseDriverFactory-style class.
        kotlin.targets.configureEach {
            compilations.configureEach {
                compileTaskProvider.configure {
                    compilerOptions {
                        freeCompilerArgs.add("-Xexpect-actual-classes")
                    }
                }
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

        // commonTest gets the KMP-portable test stack — Kotest engine +
        // matchers + Turbine + kotlinx-coroutines-test + Mokkery's
        // runtime. Specs here compile against every target (Android +
        // iOS) and run on each.
        //
        // Mokkery is a Kotlin compiler plugin (applied above) that
        // generates mock implementations of interfaces at compile time,
        // similar in spirit to MockK but K/N-native. Use `mock<T>()`,
        // `every { ... } returns ...`, `verify { ... }` etc. directly
        // in commonTest. Full JVM `mockk` still lives in androidUnitTest
        // (below) for tests that specifically need a MockK-only feature.
        kotlin.sourceSets.named("commonTest") {
            dependencies {
                implementation(libs.findLibrary("kotlin-test").get())
                implementation(libs.findLibrary("kotlinx-coroutines-test").get())
                implementation(libs.findLibrary("kotest-framework-engine").get())
                implementation(libs.findLibrary("kotest-assertions-core").get())
                implementation(libs.findLibrary("turbine").get())
                implementation(libs.findLibrary("mokkery-runtime").get())
            }
        }

        // androidUnitTest adds the JVM-only pieces on top:
        //   - kotest-runner-junit5 — bridges Kotest specs into the
        //     JUnit 5 test platform Gradle runs.
        //   - mockk — full JVM byte-code mocking. Use here when a test
        //     specifically needs a feature Mokkery doesn't ship
        //     (advanced spy/argument-capture flows).
        kotlin.sourceSets.named("androidUnitTest") {
            dependencies {
                implementation(libs.findLibrary("kotest-runner-junit5").get())
                implementation(libs.findLibrary("mockk").get())
            }
        }

        // JUnit 5 runner — Kotest's `FunSpec` / `DescribeSpec` discovery
        // depends on `useJUnitPlatform()`. Without this Gradle picks the
        // JUnit 4 runner and silently skips every test class.
        tasks.withType(org.gradle.api.tasks.testing.Test::class.java).configureEach {
            useJUnitPlatform()
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
