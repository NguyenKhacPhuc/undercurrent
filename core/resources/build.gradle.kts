// KMP + Compose Multiplatform library. Use for shared UI primitives
// (loading, error, dialog), design tokens (colors, typography),
// resources (strings, drawables), and navigation primitives.

plugins {
    alias(libs.plugins.undercurrent.kmp.library)
    alias(libs.plugins.undercurrent.kmp.library.compose)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            // module-specific deps go here
        }
    }
}

android {
    namespace = "dev.weft.undercurrent.core.resources"
}
