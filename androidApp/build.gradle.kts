// :androidApp — the Android entrypoint app. Owns Application,
// MainActivity, AndroidManifest, app-icon, Android-specific theming
// integration. Hosts the shared :composeApp inside setContent { }.
//
// Slim by design — everything that isn't strictly Android-specific
// lives in :composeApp, :shared, or the feature modules. iOS gets
// the equivalent shell in iosApp/ (Xcode project).

import java.util.Properties

plugins {
    alias(libs.plugins.undercurrent.android.application)
    alias(libs.plugins.kotlin.serialization)
}

// Tavily key for the web_search tool. Resolution order: local.properties
// (`tavily.apiKey=…`), then the TAVILY_API_KEY env var, then blank. A
// blank key is fine — WebSearchTool degrades to a NO_API_KEY error.
val tavilyApiKey: String = run {
    val props = Properties()
    rootProject.file("local.properties")
        .takeIf { it.exists() }
        ?.inputStream()
        ?.use { props.load(it) }
    props.getProperty("tavily.apiKey")
        ?: System.getenv("TAVILY_API_KEY")
        ?: ""
}

// versionCode derives from CI (the TeamCity build counter, passed as
// VERSION_CODE) so auto-deploys never collide on an already-uploaded code.
// Defaults to 1 locally. versionName likewise overridable for tagged releases.
val appVersionCode = (System.getenv("VERSION_CODE") ?: (findProperty("versionCode") as String?))
    ?.toIntOrNull() ?: 1
val appVersionName = System.getenv("VERSION_NAME") ?: (findProperty("versionName") as String?) ?: "0.0.1"

android {
    namespace = "dev.weft.undercurrent"
    defaultConfig {
        applicationId = "dev.weft.undercurrent"
        versionCode = appVersionCode
        versionName = appVersionName
        buildConfigField("String", "TAVILY_API_KEY", "\"$tavilyApiKey\"")
    }

    // Release signing — ONLY configured when a keystore is provided (CI deploy
    // builds). Absent for normal dev/CI builds, so they're unaffected. Values
    // come from env vars (TeamCity secure params) or local.properties.
    val releaseKeystore = System.getenv("RELEASE_KEYSTORE")
        ?: (findProperty("release.keystore") as String?)
    if (releaseKeystore != null) {
        signingConfigs {
            create("release") {
                storeFile = file(releaseKeystore)
                storePassword = System.getenv("RELEASE_KEYSTORE_PASSWORD")
                    ?: (findProperty("release.keystore.password") as String?)
                keyAlias = System.getenv("RELEASE_KEY_ALIAS")
                    ?: (findProperty("release.key.alias") as String?)
                keyPassword = System.getenv("RELEASE_KEY_PASSWORD")
                    ?: (findProperty("release.key.password") as String?)
            }
        }
        buildTypes {
            getByName("release") {
                signingConfig = signingConfigs.getByName("release")
            }
        }
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
    implementation(projects.core.resources)

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
    implementation(projects.feature.creator)
    implementation(projects.feature.theme)
    implementation(projects.feature.miniapps)

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
    implementation(libs.compose.multiplatform.resources)
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
