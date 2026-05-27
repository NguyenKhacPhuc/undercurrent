// Convention plugins for Undercurrent's KMP modular setup. Patterned
// after r10-android's build-logic. Plugin IDs live in the root version
// catalog (`gradle/libs.versions.toml`) under the `[plugins]` table; each
// module activates one via `plugins { alias(libs.plugins.undercurrent.kmp.feature) }`.
//
// Why a separate composite build:
//   - Convention plugins are themselves Kotlin code that depends on the
//     Android / Compose / Kotlin Gradle plugins. They have to compile
//     before any consumer build.gradle.kts can apply them.
//   - Composite-build isolation means the plugins' compile classpath
//     doesn't pollute consumer modules.

import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    `kotlin-dsl`
}

group = "dev.weft.undercurrent.buildlogic"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

dependencies {
    // compileOnly because at runtime the consumer module already has these
    // on its classpath (via plugin application). Duplicating them here would
    // cause classloader collisions.
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.compose.gradlePlugin)
    compileOnly(libs.kotlin.compose.gradlePlugin)
}

gradlePlugin {
    plugins {
        register("kmpLibrary") {
            id = "undercurrent.kmp.library"
            implementationClass = "dev.weft.undercurrent.buildlogic.KmpLibraryConventionPlugin"
        }
        register("kmpLibraryCompose") {
            id = "undercurrent.kmp.library.compose"
            implementationClass = "dev.weft.undercurrent.buildlogic.KmpLibraryComposeConventionPlugin"
        }
        register("kmpKoin") {
            id = "undercurrent.kmp.koin"
            implementationClass = "dev.weft.undercurrent.buildlogic.KmpKoinConventionPlugin"
        }
        register("kmpFeature") {
            id = "undercurrent.kmp.feature"
            implementationClass = "dev.weft.undercurrent.buildlogic.KmpFeatureConventionPlugin"
        }
        register("kmpApplication") {
            id = "undercurrent.kmp.application"
            implementationClass = "dev.weft.undercurrent.buildlogic.KmpApplicationConventionPlugin"
        }
        register("androidApplication") {
            id = "undercurrent.android.application"
            implementationClass = "dev.weft.undercurrent.buildlogic.AndroidApplicationConventionPlugin"
        }
    }
}
