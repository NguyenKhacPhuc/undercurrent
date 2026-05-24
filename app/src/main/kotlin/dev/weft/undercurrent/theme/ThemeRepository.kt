package dev.weft.undercurrent.theme

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

/**
 * Application-scoped DataStore instance for theme prefs. The delegate
 * caches the DataStore per Context — calling [Context.themeDataStore]
 * multiple times returns the same instance.
 *
 * Stored under the file `theme_prefs.preferences_pb` in the app's data dir.
 */
private val Context.themeDataStore: DataStore<Preferences> by preferencesDataStore(name = "theme_prefs")

/**
 * Reads and writes [ThemePrefs] via DataStore-Preferences. Construction
 * happens once in [UndercurrentApp]; [AppStore] consumes [prefsFlow] for
 * reactive updates and calls [setPalette] / [setMode] on intent dispatch.
 *
 * Unknown enum values (e.g. a stored palette name from a future version
 * after a downgrade) silently fall back to defaults rather than crashing.
 */
internal class ThemeRepository(context: Context) {
    private val dataStore = context.applicationContext.themeDataStore

    val prefsFlow: Flow<ThemePrefs> = dataStore.data
        .catch { /* corrupted file etc. → fall back to defaults */ emit(emptyPrefs()) }
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

    private fun emptyPrefs(): Preferences =
        androidx.datastore.preferences.core.emptyPreferences()

    private companion object {
        val KeyPalette = stringPreferencesKey("palette")
        val KeyMode = stringPreferencesKey("mode")
    }
}
