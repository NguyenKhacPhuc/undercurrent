// Pure KMP library — preferences persistence for both Android + iOS.
//
// DataStore-Preferences ships a KMP `core` artifact + platform-specific
// extensions. commonMain holds the repository classes (taking a
// DataStore<Preferences> by constructor) plus a path-based factory.
// Each platform's DI layer constructs the platform-appropriate file
// location and hands the DataStore to the repository.

plugins {
    alias(libs.plugins.undercurrent.kmp.library)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            // ThemeRepository, etc. — domain types live in :core:model.
            api(projects.core.model)
            // KMP DataStore-Preferences core. Use commonMain createDataStore
            // factory + platform path producers (androidMain → filesDir;
            // iosMain → NSDocumentDirectory).
            api(libs.androidx.datastore.preferences.core)
            // okio.Path — supplied transitively by datastore-preferences-core
            // but we declare it explicitly so iOS path building can import
            // okio types without relying on a transitive that might disappear
            // in a future DataStore release.
            implementation(libs.okio)
        }
        androidMain.dependencies {
            // Android Context delegate for `Context.preferencesDataStore`
            // — convenient but optional. We use the path-based factory
            // from commonMain for portability.
        }
    }
}

android {
    namespace = "dev.weft.undercurrent.data.datastore"
}
