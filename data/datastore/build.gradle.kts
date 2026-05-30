// Pure KMP library — DataStore-Preferences infrastructure for both
// Android + iOS.
//
// DataStore-Preferences ships a KMP `core` artifact + platform-specific
// extensions. commonMain holds the path-based factory; androidMain and
// iosMain each provide a thin platform-aware overload that builds the
// path against `filesDir` / `NSDocumentDirectory` respectively. The
// repository classes that consume `DataStore<Preferences>` live in
// `:core:domain`.

plugins {
    alias(libs.plugins.undercurrent.kmp.library)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            // KMP DataStore-Preferences core. `api` because the factory
            // returns `DataStore<Preferences>` and callers need to see
            // the type without re-declaring this dep.
            api(libs.androidx.datastore.preferences.core)
            // okio.Path — supplied transitively by datastore-preferences-core
            // but we declare it explicitly so iOS path building can import
            // okio types without relying on a transitive that might disappear
            // in a future DataStore release.
            implementation(libs.okio)
        }
        androidMain.dependencies {
            // koin-android (gives androidContext()) + :core:domain for
            // the Repository FILE_NAME constants the DI module
            // references. We add Koin manually instead of via the
            // `undercurrent.kmp.koin` convention plugin because that
            // plugin also brings koin-compose into commonMain, which
            // triggers a pre-existing okio metadata-resolution issue
            // (FileSystem.SYSTEM unresolved).
            implementation(libs.koin.android)
            implementation(projects.core.domain)
        }
        iosMain.dependencies {
            implementation(libs.koin.core)
            implementation(projects.core.domain)
        }
    }
}

android {
    namespace = "dev.weft.undercurrent.data.datastore"
}
