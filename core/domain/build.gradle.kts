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
            // KMP DataStore-Preferences core. `api` because the public
            // surface of every repository takes a `DataStore<Preferences>`
            // in its constructor — consumers (DI modules) need to see the
            // type without pulling datastore-preferences-core themselves.
            api(libs.androidx.datastore.preferences.core)
        }
        androidMain.dependencies {
            implementation("dev.weft:weft-runtime")
            implementation("dev.weft:weft-compose")
            implementation("dev.weft:weft-oauth")
            implementation("dev.weft:weft-contracts")
            implementation("dev.weft:weft-harness-conversation")
            implementation("dev.weft:weft-harness-memory")
            implementation("dev.weft:weft-harness-observability")
            implementation("dev.weft:weft-harness-cost")
            implementation(libs.kotlinx.coroutines.android)
            implementation(libs.koin.android)
        }
        iosMain.dependencies {
            // module-specific iOS deps go here
        }
    }
}

android {
    namespace = "dev.weft.undercurrent.core.domain"
}
