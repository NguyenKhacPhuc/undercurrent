// Root build for the Undercurrent app. The substrate SDK is brought in via
// composite build (see `settings.gradle.kts`). This root only declares the
// plugins the app's modules use; the SDK ships its own plugin classpath
// inside the included build.

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
}

tasks.register("clean", Delete::class) {
    delete(layout.buildDirectory)
}

// AGP 8.7.3 lint has a JDK 25 incompatibility (intellij messagebus dispose).
// Disable lint-vital release tasks until AGP catches up. Mirrors the same
// guard in the Weft SDK's root build.gradle.kts.
subprojects {
    // Match every lintVital* task — AGP 8.7.3 includes Analyze, Report, and
    // the top-level lintVitalRelease aggregator, all of which trip the same
    // JDK 25 issue.
    tasks.matching { it.name.startsWith("lintVital") }
        .configureEach { enabled = false }
}
