package dev.weft.undercurrent.data.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import dev.weft.undercurrent.core.model.AppPalette
import dev.weft.undercurrent.core.model.ThemeMode
import dev.weft.undercurrent.core.model.ThemePrefs
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

/**
 * Reads and writes [ThemePrefs] via KMP DataStore-Preferences.
 * Constructor takes an already-built `DataStore<Preferences>` so
 * Android + iOS share this same class — each platform's DI module
 * supplies the appropriate DataStore.
 *
 * Unknown enum values (e.g. a stored palette name from a future
 * version after a downgrade) silently fall back to defaults rather
 * than crashing.
 *
 * KMP — commonMain. Moved from `app/.../theme/ThemeRepository.kt`.
 * The Android-only Context delegate (`Context.themeDataStore`) is
 * gone — DI builds the DataStore explicitly and hands it here.
 */
class ThemeRepository(
    private val dataStore: DataStore<Preferences>,
) {

    val prefsFlow: Flow<ThemePrefs> = dataStore.data
        .catch { /* corrupted file etc. → fall back to defaults */ emit(emptyPreferences()) }
        .map { prefs ->
            ThemePrefs(
                palette = prefs[KeyPalette]?.let { name ->
                    runCatching { AppPalette.valueOf(name) }.getOrNull()
                } ?: AppPalette.Default,
                mode = prefs[KeyMode]?.let { name ->
                    runCatching { ThemeMode.valueOf(name) }.getOrNull()
                } ?: ThemeMode.Default,
            )
        }

    suspend fun setPalette(palette: AppPalette) {
        dataStore.edit { it[KeyPalette] = palette.name }
    }

    suspend fun setMode(mode: ThemeMode) {
        dataStore.edit { it[KeyMode] = mode.name }
    }

    /**
     * DataStore filename for theme prefs. Exposed so each platform's
     * DI module can build the path consistently.
     */
    companion object {
        const val FILE_NAME: String = "theme_prefs"
        private val KeyPalette = stringPreferencesKey("palette")
        private val KeyMode = stringPreferencesKey("mode")
    }
}
