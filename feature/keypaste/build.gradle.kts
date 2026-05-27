// Feature module — KMP + Compose Multiplatform + Koin.
// Automatically depends on :core:model, :core:ui, :core:design-system,
// :core:navigation, :core:resources via the feature convention plugin.
//
// Add feature-specific dependencies (data modules, ML Kit, etc.) below.

plugins {
    alias(libs.plugins.undercurrent.kmp.feature)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            // feature-specific common deps (e.g. :data:repository)
        }
        androidMain.dependencies {
            // androidMain-only deps (e.g. :data:weft, ML Kit)
        }
    }
}

android {
    namespace = "dev.weft.undercurrent.feature.keypaste"
}
