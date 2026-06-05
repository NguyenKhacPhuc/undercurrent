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
            implementation(projects.core.domain)
            implementation(projects.feature.auth)
            // Absorbed sub-features (providers / usage / integrations):
            // MVI base + gateway mirrors, plus Today's ISO-date lookup
            // for the usage by-day chart.
            implementation(projects.shared)
            implementation(libs.kotlinx.datetime)
        }
        androidMain.dependencies {
            // Providers sub-feature — WeftProviderViewModel rebuilds the
            // WeftAgent via AgentSession + WeftAgentFactory on provider/
            // key swap.
            implementation("dev.weft:weft-runtime")
            implementation("dev.weft:weft-harness-agents")
            implementation(projects.feature.chat)
        }
    }
}

android {
    namespace = "dev.weft.undercurrent.feature.settings"
}
