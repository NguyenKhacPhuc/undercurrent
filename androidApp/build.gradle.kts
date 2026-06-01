// :androidApp — the Android entrypoint app. Owns Application,
// MainActivity, AndroidManifest, app-icon, Android-specific theming
// integration. Hosts the shared :composeApp inside setContent { }.
//
// Slim by design — everything that isn't strictly Android-specific
// lives in :composeApp, :shared, or the feature modules. iOS gets
// the equivalent shell in iosApp/ (Xcode project).

plugins {
    alias(libs.plugins.undercurrent.android.application)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "dev.weft.undercurrent"
    defaultConfig {
        applicationId = "dev.weft.undercurrent"
        versionCode = 1
        versionName = "0.0.1"
    }
    packaging {
        resources {
            // Multiple OkHttp 5.x modules each ship the same Java-9-MR
            // OSGi manifest. Pick the first; metadata is identical.
            pickFirsts += setOf(
                "META-INF/versions/9/OSGI-INF/MANIFEST.MF",
            )
        }
    }
}

dependencies {
    implementation(projects.composeApp)
    implementation(projects.shared)

    // Cross-cutting modules
    implementation(projects.core.model)
    implementation(projects.core.designSystem)
    implementation(projects.core.navigation)
    implementation(projects.core.ui)
    implementation(projects.core.ext)

    // Domain — repositories (DataStore-Preferences-backed)
    implementation(projects.core.domain)

    // Data layer — DataStore factory + databases + Weft bridge (Android-only)
    implementation(projects.data.datastore)
    implementation(projects.data.sqldelight)
    implementation(projects.data.weft)
    // Network — androidNetworkModule (Ktor OkHttp engine) registered into Koin
    implementation(projects.data.network)

    // Feature modules — used by the App() router + AppViewModel wiring
    implementation(projects.feature.chat)
    implementation(projects.feature.auth)
    implementation(projects.feature.conversations)
    implementation(projects.feature.memories)
    implementation(projects.feature.personas)
    implementation(projects.feature.traces)
    implementation(projects.feature.settings)
    implementation(projects.feature.onboarding)
    implementation(projects.feature.providers)
    implementation(projects.feature.voice)
    implementation(projects.feature.creator)
    implementation(projects.feature.theme)
    implementation(projects.feature.usage)
    implementation(projects.feature.miniapps)
    implementation(projects.feature.keypaste)
    implementation(projects.feature.integrations)

    // Weft SDK — substrate. The host references WeftRuntime, ComposeUiBridge,
    // tool base classes, and the OAuth client directly.
    implementation("dev.weft:weft-runtime")
    implementation("dev.weft:weft-compose")
    implementation("dev.weft:weft-compose-defaults")
    implementation("dev.weft:weft-oauth")

    // Coil 3 ImageLoader — built in AppModule.kt and passed to the
    // WeftComponent palette in :core:ui. Direct dep so the host can
    // reference coil3.* types without going through :core:ui's api().
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)

    // Napier — initialised in UndercurrentApp.onCreate so substrate-side
    // log calls (e.g. WeftBindings traces) route to logcat in debug.
    implementation(libs.napier)

    // Koog (LLModel + routing types referenced by AppViewModel's model-pool path)
    implementation(libs.koog.agents)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.browser)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.sqldelight.android.driver)
    implementation(libs.sqldelight.runtime)

    implementation(libs.koin.android)
    implementation(libs.koin.androidx.compose)
}
