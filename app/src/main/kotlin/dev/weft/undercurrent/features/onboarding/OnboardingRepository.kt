package dev.weft.undercurrent.features.onboarding

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

/**
 * DataStore-Preferences file for first-launch onboarding state. Kept in
 * its own file (separate from `theme_prefs`) so theme-only edits don't
 * touch this slot — easier to reason about and trivially extensible if
 * we add more first-launch flags later.
 */
private val Context.onboardingDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "onboarding_prefs",
)

/**
 * Tracks whether the user has completed the first-launch onboarding flow.
 *
 * Set true when the user advances past the final onboarding screen (or
 * taps Skip on an intermediate screen). Persisted so onboarding never
 * re-runs unless the user reinstalls or clears app data.
 */
internal class OnboardingRepository(context: Context) {
    private val dataStore = context.applicationContext.onboardingDataStore

    val completedFlow: Flow<Boolean> = dataStore.data
        .catch { /* corrupt prefs → fall back to "not completed" so onboarding shows */ emit(androidx.datastore.preferences.core.emptyPreferences()) }
        .map { prefs -> prefs[KeyCompleted] ?: false }

    suspend fun markCompleted() {
        dataStore.edit { it[KeyCompleted] = true }
    }

    private companion object {
        val KeyCompleted = booleanPreferencesKey("completed")
    }
}
