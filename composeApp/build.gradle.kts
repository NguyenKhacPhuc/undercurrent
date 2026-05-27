// :composeApp — KMP "shared app" surface. The Compose UI root that
// both :androidApp and the iOS Xcode project consume.
//
// Targets: android() + iosArm64() + iosSimulatorArm64(). Produces a
// `ComposeApp.framework` static library on iOS that iosApp embeds.
//
// What lives here:
//   - The top-level `App()` composable + theming.
//   - The screen-routing switch over AppState.screen.
//   - Cross-feature glue (snackbar host, navigation host).
//
// What does NOT live here:
//   - Feature-specific UI (lives in :feature:*).
//   - Android-only services / lifecycle hooks (lives in :androidApp).
//   - SwiftUI scenes (lives in iosApp/).

plugins {
    alias(libs.plugins.undercurrent.kmp.application)
    alias(libs.plugins.undercurrent.kmp.koin)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            // Every feature module the host needs to render. The
            // featureXxx() Koin module from each is wired up in
            // commonMain/.../di/AppModule.kt.
            implementation(projects.core.designSystem)
            implementation(projects.core.navigation)
            implementation(projects.core.ui)
            implementation(projects.core.resources)
            implementation(projects.core.model)
            implementation(projects.shared)

            implementation(projects.feature.chat)
            implementation(projects.feature.conversations)
            implementation(projects.feature.memories)
            implementation(projects.feature.personas)
            implementation(projects.feature.traces)
            implementation(projects.feature.settings)
            implementation(projects.feature.onboarding)
            implementation(projects.feature.providers)
            implementation(projects.feature.voice)
            implementation(projects.feature.maps)
            implementation(projects.feature.creator)
            implementation(projects.feature.theme)
            implementation(projects.feature.usage)
            implementation(projects.feature.miniapps)
            implementation(projects.feature.keypaste)
            implementation(projects.feature.integrations)
            implementation(projects.feature.navigation)

            implementation(projects.data.repository)
            implementation(projects.data.datastore)
            implementation(projects.data.sqldelight)
        }
        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.lifecycle.runtime.compose)
            // Weft bridge — Android-only. iOS variant ships without
            // agent capabilities for v1.
            implementation(projects.data.weft)
        }
    }
}

android {
    namespace = "dev.weft.undercurrent.composeapp"
}
