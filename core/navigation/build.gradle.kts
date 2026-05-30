// KMP + Compose Multiplatform library. Use for shared UI primitives
// (loading, error, dialog), design tokens (colors, typography),
// resources (strings, drawables), and navigation primitives.

plugins {
    alias(libs.plugins.undercurrent.kmp.library)
    alias(libs.plugins.undercurrent.kmp.library.compose)
    alias(libs.plugins.undercurrent.kmp.koin)
}

// No external nav deps — NavKey + NavBackStack are KMP-clean
// equivalents living in commonMain. See NavBackStack.kt for the
// rationale (CMP/AndroidX saveable IR conflict on K/N).

kotlin {
    sourceSets {
        commonMain.dependencies {
            // No external deps — NavigationIntent dropped its `AppIntent`
            // parent in the central-dispatch removal; NavBackStack +
            // NavigationViewModel are KMP-clean.
        }
    }
}

android {
    namespace = "dev.weft.undercurrent.core.navigation"
}
