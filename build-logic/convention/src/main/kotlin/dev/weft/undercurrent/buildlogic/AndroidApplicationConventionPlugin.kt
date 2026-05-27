package dev.weft.undercurrent.buildlogic

import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure

/**
 * `:androidApp` — the Android entrypoint app (Application class,
 * MainActivity, manifest, app-level theming). Compose-enabled. Hosts
 * the shared `:composeApp` CMP surface inside an `androidx.activity.compose`
 * setContent.
 *
 * Use only for the single `:androidApp` module. Feature modules use
 * `undercurrent.kmp.feature`.
 */
class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project): Unit = with(target) {
        apply(plugin = "com.android.application")
        apply(plugin = "org.jetbrains.kotlin.android")
        apply(plugin = "org.jetbrains.kotlin.plugin.compose")

        extensions.configure<ApplicationExtension> {
            compileSdk = androidCompileSdk
            defaultConfig {
                minSdk = androidMinSdk
                targetSdk = androidTargetSdk
            }
            buildFeatures {
                compose = true
                buildConfig = true
            }
            compileOptions {
                sourceCompatibility = JavaVersion.VERSION_17
                targetCompatibility = JavaVersion.VERSION_17
            }
            packaging {
                resources {
                    // Koog → Netty + multiple OkHttp 5.x modules ship
                    // duplicate META-INF entries. Same exclusion set
                    // the old single-module :app build used.
                    excludes += setOf(
                        "META-INF/INDEX.LIST",
                        "META-INF/io.netty.versions.properties",
                        "META-INF/DEPENDENCIES",
                        "META-INF/LICENSE",
                        "META-INF/LICENSE.txt",
                        "META-INF/NOTICE",
                        "META-INF/NOTICE.txt",
                        "META-INF/*.kotlin_module",
                        "META-INF/AL2.0",
                        "META-INF/LGPL2.1",
                    )
                }
            }
        }
    }
}
