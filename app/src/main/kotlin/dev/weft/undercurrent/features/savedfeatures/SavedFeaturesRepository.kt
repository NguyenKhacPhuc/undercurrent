package dev.weft.undercurrent.features.savedfeatures

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.serialization.json.Json
import java.util.UUID

private val Context.savedFeaturesDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "saved_features_prefs",
)

/**
 * Persistent store for [SavedFeature]s.
 *
 * Storage shape: one JSON-encoded list under a single key. List size
 * is small (tens of features at most for a personal app), so we
 * read/write the whole blob on every mutation — same pattern
 * [dev.weft.undercurrent.features.personas.PersonaRepository] uses
 * for custom personas. If feature counts ever grow into the
 * hundreds, swap for an SQLDelight table; the public surface here
 * doesn't need to change.
 *
 * Reads expose both a [Flow] (for screens) and a [StateFlow] (for
 * fast .value access when invoking a feature from a quick-action
 * chip). Writes are suspend.
 */
internal class SavedFeaturesRepository(context: Context) {
    private val dataStore = context.applicationContext.savedFeaturesDataStore

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val json = Json { ignoreUnknownKeys = true }

    private val featuresFlow: Flow<List<SavedFeature>> = dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { prefs -> parse(prefs[KeyFeatures]) }

    /**
     * Eagerly-collected snapshot. The chip row in the chat sheet
     * reads `.value` directly so the UI doesn't flicker between
     * empty + populated on first composition. Process-scoped because
     * the repo's lifetime is the process.
     */
    val features: StateFlow<List<SavedFeature>> = featuresFlow
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    suspend fun add(
        name: String,
        emoji: String,
        triggerPrompt: String,
    ): SavedFeature {
        val created = SavedFeature(
            id = "feature.${UUID.randomUUID().toString().take(8)}",
            name = name,
            emoji = emoji,
            triggerPrompt = triggerPrompt,
            createdAtEpochMs = System.currentTimeMillis(),
            usageCount = 0,
        )
        dataStore.edit { prefs ->
            val current = parse(prefs[KeyFeatures])
            prefs[KeyFeatures] = json.encodeToString(current + created)
        }
        return created
    }

    suspend fun update(
        id: String,
        name: String,
        emoji: String,
        triggerPrompt: String,
    ) {
        dataStore.edit { prefs ->
            val current = parse(prefs[KeyFeatures])
            val updated = current.map { f ->
                if (f.id == id) f.copy(name = name, emoji = emoji, triggerPrompt = triggerPrompt) else f
            }
            prefs[KeyFeatures] = json.encodeToString(updated)
        }
    }

    suspend fun delete(id: String) {
        dataStore.edit { prefs ->
            val current = parse(prefs[KeyFeatures])
            prefs[KeyFeatures] = json.encodeToString(current.filterNot { it.id == id })
        }
    }

    /**
     * Persist the latest agent-rendered tree on [id] so the next
     * invocation can show it instantly. Called from [AppStore] when
     * a `UIUpdate.RenderTree` lands during a feature-invoked turn.
     * No-op if [id] no longer exists.
     */
    suspend fun setCachedRender(id: String, treeJson: String) {
        dataStore.edit { prefs ->
            val current = parse(prefs[KeyFeatures])
            val updated = current.map { f ->
                if (f.id == id) f.copy(
                    lastRenderTreeJson = treeJson,
                    lastRenderedAtEpochMs = System.currentTimeMillis(),
                ) else f
            }
            prefs[KeyFeatures] = json.encodeToString(updated)
        }
    }

    /**
     * Bump [SavedFeature.usageCount] for [id]. Called from the chip
     * tap path — drives the most-used-first sort in the sheet row.
     * No-op if the id no longer exists (race with a delete from
     * another surface).
     */
    suspend fun recordUsage(id: String) {
        dataStore.edit { prefs ->
            val current = parse(prefs[KeyFeatures])
            val updated = current.map { f ->
                if (f.id == id) f.copy(usageCount = f.usageCount + 1) else f
            }
            prefs[KeyFeatures] = json.encodeToString(updated)
        }
    }

    private fun parse(raw: String?): List<SavedFeature> {
        if (raw.isNullOrEmpty()) return emptyList()
        return runCatching {
            json.decodeFromString<List<SavedFeature>>(raw)
        }.getOrDefault(emptyList())
    }

    private companion object {
        val KeyFeatures = stringPreferencesKey("features")
    }
}
