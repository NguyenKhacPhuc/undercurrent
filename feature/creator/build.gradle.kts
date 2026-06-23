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
            // `AppIntent` parent marker — CreatorIntent extends it.
            implementation(projects.core.domain)
            implementation(projects.shared)
            // CreatorScreen reuses the chat feature's live ActivityIndicator
            // for an alive "still working" cue during long generations.
            implementation(projects.feature.chat)
        }
        androidMain.dependencies {
            // Substrate — WeftCreatorViewModel drives WeftAgent via
            // AgentSession + ChatViewModel.send.
            implementation("dev.weft:weft-harness-agents")
        }
    }
}

android {
    namespace = "dev.weft.undercurrent.feature.creator"
}
