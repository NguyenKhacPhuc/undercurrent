package dev.weft.undercurrent.data.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

/**
 * Tracks whether the user has completed the first-launch onboarding
 * flow. Persisted in a separate DataStore file so theme-only writes
 * don't touch this slot.
 *
 * Construction: DI module supplies the `DataStore<Preferences>`
 * — Android uses `createPreferencesDataStore(context, FILE_NAME)`,
 * iOS uses `createPreferencesDataStore(FILE_NAME)`.
 *
 * KMP — commonMain. Moved from
 * `app/.../features/onboarding/OnboardingRepository.kt`.
 */
public class OnboardingRepository(
    private val dataStore: DataStore<Preferences>,
) {

    public val completedFlow: Flow<Boolean> = dataStore.data
        .catch { /* corrupt prefs → "not completed" so onboarding shows */ emit(emptyPreferences()) }
        .map { prefs -> prefs[KeyCompleted] ?: false }

    public suspend fun markCompleted() {
        dataStore.edit { it[KeyCompleted] = true }
    }

    public companion object {
        public const val FILE_NAME: String = "onboarding_prefs"
        private val KeyCompleted = booleanPreferencesKey("completed")
    }
}
