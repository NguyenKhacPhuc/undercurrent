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
            // MiniAppsRepository (cached tree JSON + usage counters).
            implementation(projects.core.domain)
            // Store<State, Intent, Effect> MVI base.
            implementation(projects.shared)
        }
        androidMain.dependencies {
            // Substrate — WeftMiniAppViewModel seeds the cached
            // render tree into WeftRuntime.uiBridge on invocation.
            // The chat-send capability is injected as a lambda
            // (see miniAppAndroidModule) so this module does NOT
            // depend on :feature:chat — that would cycle.
            implementation("dev.weft:weft-runtime")
            implementation("dev.weft:weft-contracts")
        }
    }
}

android {
    namespace = "dev.weft.undercurrent.feature.miniapps"
}
