package dev.weft.undercurrent.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences

/**
 * Android factory for KMP DataStore-Preferences. Builds the file path
 * under `context.filesDir/` so the on-disk location matches what the
 * legacy `Context.preferencesDataStore(name)` delegate produced — apps
 * upgrading from the old single-module layout keep their stored prefs.
 *
 * Use in the androidMain DI module:
 *
 * ```kotlin
 * single<DataStore<Preferences>>(named("theme")) {
 *     createPreferencesDataStore(
 *         context = androidContext(),
 *         name = ThemeRepository.FILE_NAME,
 *     )
 * }
 * single { ThemeRepository(get(named("theme"))) }
 * ```
 */
fun createPreferencesDataStore(
    context: Context,
    name: String,
): DataStore<Preferences> =
    createPreferencesDataStore(
        producePath = {
            // Matches the path the old `preferencesDataStore` delegate
            // used: `context.filesDir/datastore/<name>.preferences_pb`.
            // We elide the "datastore/" subdir for simplicity — the
            // suffix is the only thing tools care about.
            context.filesDir.resolve("$name$DATA_STORE_FILE_EXT").absolutePath
        }
    )
