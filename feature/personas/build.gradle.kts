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
            // PersonaRepository (active voice / role + custom personas).
            implementation(projects.core.domain)
            // Store<State, Intent, Effect> MVI base + the gateway
            // interfaces in :shared/commonMain.
            implementation(projects.shared)
        }
        androidMain.dependencies {
            // androidMain-only deps (e.g. :data:weft, ML Kit)
        }
    }
}

android {
    namespace = "dev.weft.undercurrent.feature.personas"
}
