// Root build for the Undercurrent app — KMP-modular layout. The
// substrate SDK lives in `../weft` (composite build via
// `settings.gradle.kts`). This root only declares the plugins the
// app's modules use (`apply false` — each module applies what it
// needs via convention plugins from `build-logic/`).

plugins {
    // Stock plugins — applied per-module via convention plugins or
    // directly in module build.gradle.kts. `apply false` here just
    // pins the version on the root classpath.
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.compose.multiplatform) apply false
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.sqldelight) apply false
    alias(libs.plugins.kover)
}

subprojects {
    apply(plugin = rootProject.libs.plugins.kover.get().pluginId)
}

dependencies {
    subprojects.forEach { kover(project(it.path)) }
}

kover {
    reports {
        filters {
            includes {
                classes(
                    "dev.weft.undercurrent.*ViewModel",
                    "dev.weft.undercurrent.*UseCase",
                )
            }
        }
    }
}

// When weft is consumed as a published artifact (CI — no `../weft`
// composite build), the versionless `dev.weft:weft-*` coordinates need a
// version. The composite `dependencySubstitution` overrides this locally,
// so this rule only bites when resolving from GitHub Packages.
val weftVersion = (findProperty("weftVersion") as String?) ?: "0.0.1"
subprojects {
    configurations.configureEach {
        resolutionStrategy.eachDependency {
            if (requested.group == "dev.weft" || requested.group == "dev.weft.devtools") {
                useVersion(weftVersion)
            }
        }
    }
}

tasks.register("clean", Delete::class) {
    delete(layout.buildDirectory)
}

// AGP 8.7.3 lint has a JDK 25 incompatibility (intellij messagebus
// dispose). Disable lintVital* tasks until AGP catches up. Mirrors
// the same guard in the Weft SDK's root build.gradle.kts.
subprojects {
    tasks.matching { it.name.startsWith("lintVital") }
        .configureEach { enabled = false }
}
