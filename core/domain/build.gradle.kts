// Pure KMP library — Android + iOS, no Compose, no Koin.
//
// Holds repository classes (DataStore-Preferences-backed) and, in
// future, use cases. Repositories take a `DataStore<Preferences>` by
// constructor — the data layer at `:data:datastore` builds the
// platform-appropriate DataStore and DI hands it here.

plugins {
    alias(libs.plugins.undercurrent.kmp.library)
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
            // module-specific Android deps go here
        }
        iosMain.dependencies {
            // module-specific iOS deps go here
        }
    }
}

android {
    namespace = "dev.weft.undercurrent.core.domain"
}
