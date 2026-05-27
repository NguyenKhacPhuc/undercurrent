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
            // Theme tokens + UndercurrentTheme accessor.
            api(projects.core.designSystem)
        }
    }
}

android {
    namespace = "dev.weft.undercurrent.core.ui"
}
