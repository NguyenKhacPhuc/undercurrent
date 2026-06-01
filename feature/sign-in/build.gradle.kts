// Feature module — KMP + Compose Multiplatform + Koin.
// Automatically depends on :core:model, :core:ui, :core:design-system,
// :core:navigation, :core:resources via the feature convention plugin.
//
// Add feature-specific dependencies (data modules, ML Kit, etc.) below.

plugins {
    alias(libs.plugins.undercurrent.kmp.feature)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            // MviViewModel base class.
            implementation(projects.shared)
            // AuthRepository (signUp / signIn / getMe / signOut) +
            // SessionTokenStore (persist the bearer after success).
            implementation(projects.core.domain)
            // ApiException + NetworkException — surfaced by AuthRepository,
            // pattern-matched by the ViewModel to produce inline UI errors.
            implementation(projects.data.network)
        }
    }
}

android {
    namespace = "dev.weft.undercurrent.feature.signin"
}
