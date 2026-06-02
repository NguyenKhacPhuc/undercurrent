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

            implementation(projects.core.domain)
            implementation(projects.data.datastore)
            implementation(projects.data.sqldelight)
            // Network — iosNetworkModule (Ktor Darwin engine) registered into Koin
            implementation(projects.data.network)
            // Sign-in feature — SignInRoute referenced by ScreenRouter
            implementation(projects.feature.auth)
        }
        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.lifecycle.runtime.compose)
            // Weft bridge — Android-only. iOS variant ships without
            // agent capabilities for v1.
            implementation(projects.data.weft)
        }
        iosMain.dependencies {
            // The substrate's KMP agent loop. Every Koog-using harness
            // module + WeftAgent publishes iOS klibs. As of the
            // weft-ios-parity work the composition root itself is KMP, so
            // iOS now stands up the agent through `WeftRuntime.create(
            // WeftPlatform(), …)` (the turnkey factory) rather than
            // instantiating WeftAgent by hand.
            implementation("dev.weft:weft-runtime")
            implementation("dev.weft:weft-compose")
            // OAuth (iOS launcher + token store) for the integrations
            // sign-in wiring.
            implementation("dev.weft:weft-oauth")
            implementation("dev.weft:weft-harness-agents")
            implementation("dev.weft:weft-harness-prompt")
            implementation("dev.weft:weft-harness-observability")
            implementation("dev.weft:weft-harness-conversation")
            implementation("dev.weft:weft-harness-cost")
            implementation("dev.weft:weft-harness-memory")
            implementation("dev.weft:weft-harness-reliability")
            implementation("dev.weft:weft-harness-behavior")
            implementation("dev.weft:weft-tools")

            // Ktor — still needed: the parallel LlmClient path is kept
            // alongside the substrate path during the migration so
            // current iOS chat keeps working. Plus Koog's
            // KtorKoogHttpClient.Factory needs a Ktor engine to talk
            // to providers — we pass it explicitly because the
            // ServiceLoader-based discovery doesn't apply to K/N.
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.darwin)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            // SQLDelight reactive queries — used by IosConversationStoreGateway
            // to expose `asFlow().mapToList(...)` against the conversations
            // table.
            implementation(libs.sqldelight.coroutines.extensions)

            // Napier — initialised in InitKoin.doInitKoin() so the
            // substrate's logger calls route to NSLog / Xcode console.
            implementation(libs.napier)

            // Coil 3's KMP-friendly network fetcher. Pairs with the
            // existing ktor-client-darwin engine to enable image
            // loading inside iosMain WeftComponents (PhotoFrame /
            // Avatar / hero-style). Wired in IosKoinModule via
            // ImageLoader.Builder { components { add(...) } }.
            implementation(libs.coil.network.ktor3)
        }
    }
}

android {
    namespace = "dev.weft.undercurrent.composeapp"
}
