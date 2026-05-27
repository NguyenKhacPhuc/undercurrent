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
