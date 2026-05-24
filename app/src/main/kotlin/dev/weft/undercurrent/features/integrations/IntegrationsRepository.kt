package dev.weft.undercurrent.features.integrations

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * Persisted set of integration ids the user has connected. OAuth tokens
 * themselves live in [dev.weft.oauth.OAuthTokenStore] (encrypted via
 * KeyVault); this repo only tracks which connectors are "active" — used
 * to decide which MCP servers to register at runtime construction time.
 *
 * Separate file from `theme_prefs` / `onboarding_prefs` so a stray reset
 * of one slot doesn't blow away the others.
 */
private val Context.integrationsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "integrations_prefs",
)

internal class IntegrationsRepository(context: Context) {
    private val dataStore = context.applicationContext.integrationsDataStore

    val enabledIdsFlow: Flow<Set<String>> = dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { prefs -> prefs[KeyEnabledIds] ?: emptySet() }

    /**
     * Synchronous read used by the Koin runtime factory at app boot.
     * Blocking is acceptable here — the factory is already synchronous
     * (cold DB open, key vault init) and DataStore's first read is fast
     * (single file, no migrations). If this ever shows up in startup
     * profiles, swap callers to `enabledIdsFlow.first()` from an IO
     * coroutine and pass the result in.
     */
    suspend fun enabledIdsNow(): Set<String> = enabledIdsFlow.first()

    suspend fun setEnabled(id: String, enabled: Boolean) {
        dataStore.edit { prefs ->
            val current = prefs[KeyEnabledIds] ?: emptySet()
            prefs[KeyEnabledIds] = if (enabled) current + id else current - id
        }
    }

    private companion object {
        val KeyEnabledIds = stringSetPreferencesKey("enabled_ids")
    }
}
