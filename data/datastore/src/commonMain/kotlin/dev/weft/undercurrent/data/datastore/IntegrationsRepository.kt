package dev.weft.undercurrent.data.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * Persisted set of integration ids the user has connected (Linear,
 * Gmail, GitHub, etc.). The OAuth tokens themselves live in Weft's
 * `OAuthTokenStore` (KeyVault-backed); this repo only tracks the
 * which-is-active flag used to decide which MCP servers to register
 * at runtime construction time.
 *
 * KMP — commonMain. Moved from
 * `app/.../features/integrations/IntegrationsRepository.kt`.
 */
class IntegrationsRepository(
    private val dataStore: DataStore<Preferences>,
) {

    val enabledIdsFlow: Flow<Set<String>> = dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { prefs -> prefs[KeyEnabledIds] ?: emptySet() }

    /**
     * Suspending snapshot used by the Koin runtime factory at boot.
     * Single DataStore read; fast on warm process. If this ever shows
     * in startup profiles, swap callers to consume `enabledIdsFlow`
     * from an IO coroutine.
     */
    suspend fun enabledIdsNow(): Set<String> = enabledIdsFlow.first()

    suspend fun setEnabled(id: String, enabled: Boolean) {
        dataStore.edit { prefs ->
            val current = prefs[KeyEnabledIds] ?: emptySet()
            prefs[KeyEnabledIds] = if (enabled) current + id else current - id
        }
    }

    companion object {
        const val FILE_NAME: String = "integrations_prefs"
        private val KeyEnabledIds = stringSetPreferencesKey("enabled_ids")
    }
}
