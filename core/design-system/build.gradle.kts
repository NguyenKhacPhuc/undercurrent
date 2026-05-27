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
            // AppPalette enum lives in :core:model (Compose-free). The
            // color tables backing it + the `colors(dark)` extension
            // are this module's responsibility.
            api(projects.core.model)
        }
    }
}

android {
    namespace = "dev.weft.undercurrent.core.designsystem"
}
