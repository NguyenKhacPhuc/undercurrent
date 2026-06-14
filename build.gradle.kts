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
    alias(libs.plugins.google.firebase.appdistribution) apply false
    alias(libs.plugins.google.gms.google.services) apply false
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

// When weft is consumed as a published artifact (CI — no `../weft` composite
// build), the versionless `dev.weft:weft-*` coordinates need a version. ONLY
// apply this in artifact mode: in composite mode it forces versions on weft's
// transitive internal modules, which breaks the composite auto-substitution
// (project :weft:security -> dev.weft:security:0.0.1 FAILED) and IDE sync.
val weftVersion = (findProperty("weftVersion") as String?) ?: "0.0.1"
val weftAsArtifacts = !file("../weft").exists() ||
    providers.gradleProperty("weft.useArtifacts").orNull == "true"
if (weftAsArtifacts) {
    subprojects {
        configurations.configureEach {
            resolutionStrategy.eachDependency {
                if (requested.group == "dev.weft" || requested.group == "dev.weft.devtools") {
                    useVersion(weftVersion)
                }
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

// Lint baseline: CI gates only NEW lint errors. Regenerate with
// `./gradlew updateLintBaseline`. Mirrors the Weft SDK's setup.
subprojects {
    pluginManager.withPlugin("com.android.library") {
        extensions.configure<com.android.build.api.dsl.LibraryExtension> {
            lint { baseline = file("lint-baseline.xml") }
        }
    }
    pluginManager.withPlugin("com.android.application") {
        extensions.configure<com.android.build.api.dsl.ApplicationExtension> {
            lint { baseline = file("lint-baseline.xml") }
        }
    }
}

// Gradle 9 fails a JVM test task that has sources but discovers no tests.
// Some KMP modules compile shared specs into a test task that legitimately
// runs zero. Don't fail on that.
subprojects {
    tasks.withType<Test>().configureEach {
        failOnNoDiscoveredTests = false
    }
}
