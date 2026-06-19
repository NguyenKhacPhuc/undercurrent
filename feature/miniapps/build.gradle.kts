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
            // Mini-app bridge contracts (MiniAppStateStore, MiniAppScopeResolver)
            // the host catalog binds the HTML mini-app runtime against.
            implementation("dev.weft:weft-compose-defaults")
            // ComponentNode — the render tree emitted on tap for an HTML mini-app.
            implementation("dev.weft:weft-contracts")
            // http_fetch action handler runs against a host-supplied client
            // (the host installs its NetworkPolicy allowlist plugin on it).
            implementation(libs.ktor.client.core)
        }
        commonTest.dependencies {
            implementation(libs.ktor.client.mock)
        }
    }
}

android {
    namespace = "dev.weft.undercurrent.feature.miniapps"
}
