// KMP library — repository classes + their Koin bindings. Use cases
// are declared per-feature in the consuming feature modules.

plugins {
    alias(libs.plugins.undercurrent.kmp.library)
    alias(libs.plugins.undercurrent.kmp.koin)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            // Domain types — Persona, MiniApp, ThemePrefs, ProviderKind, …
            api(projects.core.model)
            // Result + asResult — used by AuthRepository's Flow<Result<T>>
            // surface. `api` because Result appears in public method
            // signatures of repositories.
            api(projects.core.ext)
            // safeApiCallRaw + PlatformHttpClientEngineFactory — used by
            // AuthRepositoryImpl + the default auth HttpClient factory.
            implementation(projects.data.network)
            // KMP DataStore-Preferences core. `api` because the public
            // surface of every repository takes a `DataStore<Preferences>`
            // in its constructor — consumers (DI modules) need to see the
            // type without pulling datastore-preferences-core themselves.
            api(libs.androidx.datastore.preferences.core)
            // Ktor — auth repository implementation talks to the BE.
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.kotlinx.serialization.json)
            // Substrate stores — shared now that WeftRuntime is KMP, so the
            // Weft-backed history/memory/trace/usage repositories live in
            // commonMain (Android + iOS) rather than androidMain only.
            implementation("dev.weft:weft-harness-conversation")
            implementation("dev.weft:weft-harness-memory")
            implementation("dev.weft:weft-harness-observability")
            implementation("dev.weft:weft-harness-cost")
            // KeyVault contract + the runtime's per-provider key aliases —
            // needed by the shared WeftKeyVaultRepository.
            implementation("dev.weft:weft-contracts")
            implementation("dev.weft:weft-runtime")
        }
        commonTest.dependencies {
            // MockEngine for AuthRepositoryImpl tests (mobile-auth-wiring/04).
            implementation(libs.ktor.client.mock)
        }
        androidMain.dependencies {
            implementation("dev.weft:weft-compose")
            implementation("dev.weft:weft-oauth")
            implementation(libs.kotlinx.coroutines.android)
            implementation(libs.koin.android)
            // EncryptedSharedPreferences for the BE session bearer (mobile-auth-wiring/02).
            implementation(libs.androidx.security.crypto)
        }
        iosMain.dependencies {
            // module-specific iOS deps go here
        }
    }
}

android {
    namespace = "dev.weft.undercurrent.core.domain"
}
