package dev.weft.undercurrent.features.miniapps

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

// DataStore file name kept as `saved_features_prefs` deliberately —
// renaming would force a migration with no user-visible benefit (this
// file is an implementation detail; the user-facing name is what
// matters). Existing installs continue without data loss.
private val Context.miniAppsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "saved_features_prefs",
)

/**
 * Persistent store for [MiniApp]s.
 *
 * Storage shape: one JSON-encoded list under a single key. List size
 * is small (tens of mini-apps at most for a personal app), so we
 * read/write the whole blob on every mutation — same pattern
 * [dev.weft.undercurrent.features.personas.PersonaRepository] uses
 * for custom personas. If counts ever grow into the hundreds, swap
 * for an SQLDelight table; the public surface here doesn't change.
 *
 * Reads expose both a [Flow] (for screens) and a [StateFlow] (for
 * fast .value access when invoking a mini-app from a quick-action
 * chip). Writes are suspend.
 */
internal class MiniAppsRepository(context: Context) {
    private val dataStore = context.applicationContext.miniAppsDataStore

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val json = Json { ignoreUnknownKeys = true }

    private val miniAppsFlow: Flow<List<MiniApp>> = dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { prefs -> parse(prefs[KeyMiniApps]) }

    /**
     * Eagerly-collected snapshot. The chip row in the chat sheet
     * reads `.value` directly so the UI doesn't flicker between
     * empty + populated on first composition. Process-scoped because
     * the repo's lifetime is the process.
     */
    val miniApps: StateFlow<List<MiniApp>> = miniAppsFlow
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    suspend fun add(
        name: String,
        emoji: String,
        triggerPrompt: String,
    ): MiniApp {
        // `feature.` id prefix kept for back-compat with existing
        // persisted rows. See MiniApp.id KDoc.
        val created = MiniApp(
            id = "feature.${UUID.randomUUID().toString().take(8)}",
            name = name,
            emoji = emoji,
            triggerPrompt = triggerPrompt,
            createdAtEpochMs = System.currentTimeMillis(),
            usageCount = 0,
        )
        dataStore.edit { prefs ->
            val current = parse(prefs[KeyMiniApps])
            prefs[KeyMiniApps] = json.encodeToString(current + created)
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
            val current = parse(prefs[KeyMiniApps])
            val updated = current.map { f ->
                if (f.id == id) f.copy(name = name, emoji = emoji, triggerPrompt = triggerPrompt) else f
            }
            prefs[KeyMiniApps] = json.encodeToString(updated)
        }
    }

    suspend fun delete(id: String) {
        dataStore.edit { prefs ->
            val current = parse(prefs[KeyMiniApps])
            prefs[KeyMiniApps] = json.encodeToString(current.filterNot { it.id == id })
        }
    }

    /**
     * Persist the latest agent-rendered tree on [id] so the next
     * invocation can show it instantly. Called from [AppStore] when
     * a `UIUpdate.RenderTree` lands during a mini-app-invoked turn.
     * No-op if [id] no longer exists.
     */
    suspend fun setCachedRender(id: String, treeJson: String) {
        dataStore.edit { prefs ->
            val current = parse(prefs[KeyMiniApps])
            val updated = current.map { f ->
                if (f.id == id) f.copy(
                    lastRenderTreeJson = treeJson,
                    lastRenderedAtEpochMs = System.currentTimeMillis(),
                ) else f
            }
            prefs[KeyMiniApps] = json.encodeToString(updated)
        }
    }

    /**
     * Bump [MiniApp.usageCount] for [id]. Called from the chip tap
     * path — drives the most-used-first sort in the sheet row. No-op
     * if the id no longer exists (race with a delete from another
     * surface).
     */
    suspend fun recordUsage(id: String) {
        dataStore.edit { prefs ->
            val current = parse(prefs[KeyMiniApps])
            val updated = current.map { f ->
                if (f.id == id) f.copy(usageCount = f.usageCount + 1) else f
            }
            prefs[KeyMiniApps] = json.encodeToString(updated)
        }
    }

    private fun parse(raw: String?): List<MiniApp> {
        if (raw.isNullOrEmpty()) return emptyList()
        return runCatching {
            json.decodeFromString<List<MiniApp>>(raw)
        }.getOrDefault(emptyList())
    }

    private companion object {
        // Key name `features` kept for back-compat with the old
        // SavedFeaturesRepository — see file-level comment.
        val KeyMiniApps = stringPreferencesKey("features")
    }
}
