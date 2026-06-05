package dev.weft.undercurrent.core.domain

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import dev.weft.undercurrent.core.model.MiniApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Persistent store for [MiniApp]s.
 *
 * Storage shape: one JSON-encoded list under a single key. List size
 * is small (tens of mini-apps for a personal app), so we read/write
 * the whole blob on every mutation. If counts grow into hundreds,
 * swap for an SQLDelight table; the public surface here doesn't
 * change.
 *
 * Reads expose both a [Flow] (for screens) and a [StateFlow] (for
 * fast .value access when invoking a mini-app from a chip).
 *
 * KMP — commonMain. Moved from
 * `app/.../features/miniapps/MiniAppsRepository.kt`. Adjustments:
 *   - `System.currentTimeMillis()` → `kotlin.time.Clock.System.now().toEpochMilliseconds()`
 *   - `java.util.UUID` → `kotlin.uuid.Uuid`
 *   - Constructor-injected `DataStore<Preferences>` instead of the
 *     Android Context delegate.
 *
 * Persisted file name kept as `saved_features_prefs` (legacy of the
 * old "saved features" naming) so installs upgrading from the old
 * single-module app don't lose data.
 */
@OptIn(ExperimentalUuidApi::class, ExperimentalTime::class)
class MiniAppsRepository(
    private val dataStore: DataStore<Preferences>,
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val json = Json { ignoreUnknownKeys = true }

    private val miniAppsFlow: Flow<List<MiniApp>> = dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { prefs -> parse(prefs[KeyMiniApps]) }

    val miniApps: StateFlow<List<MiniApp>> = miniAppsFlow
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    suspend fun add(
        name: String,
        emoji: String,
        triggerPrompt: String,
    ): MiniApp {
        val created = MiniApp(
            id = "feature.${Uuid.random().toString().take(8)}",
            name = name,
            emoji = emoji,
            triggerPrompt = triggerPrompt,
            createdAtEpochMs = Clock.System.now().toEpochMilliseconds(),
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

    suspend fun setCachedRender(id: String, treeJson: String) {
        dataStore.edit { prefs ->
            val current = parse(prefs[KeyMiniApps])
            val updated = current.map { f ->
                if (f.id == id) f.copy(
                    lastRenderTreeJson = treeJson,
                    lastRenderedAtEpochMs = Clock.System.now().toEpochMilliseconds(),
                ) else f
            }
            prefs[KeyMiniApps] = json.encodeToString(updated)
        }
    }

    suspend fun recordUsage(id: String) {
        dataStore.edit { prefs ->
            val current = parse(prefs[KeyMiniApps])
            val updated = current.map { f ->
                if (f.id == id) f.copy(usageCount = f.usageCount + 1) else f
            }
            prefs[KeyMiniApps] = json.encodeToString(updated)
        }
    }

    /** Save the HTML document a mini-app renders + the scopes it declares it needs. */
    suspend fun setHtmlDocument(id: String, html: String, declaredScopes: Set<String>) {
        mutate(id) { it.copy(htmlDocument = html, declaredScopes = declaredScopes) }
    }

    /** Record the action scopes the user approved for a mini-app (the grant). */
    suspend fun approveScopes(id: String, scopes: Set<String>) {
        mutate(id) { it.copy(approvedScopes = scopes) }
    }

    /**
     * Record the user's first-run consent decision: the [approved] set
     * (possibly empty, on deny) plus the decision timestamp, so the
     * mini-app isn't prompted again. Approving nothing is a real decision.
     */
    suspend fun recordConsent(id: String, approved: Set<String>) {
        mutate(id) {
            it.copy(
                approvedScopes = approved,
                consentedAt = Clock.System.now().toEpochMilliseconds(),
            )
        }
    }

    /** The mini-app's saved state JSON, or null if it never saved any. */
    suspend fun getState(id: String): String? =
        miniApps.value.firstOrNull { it.id == id }?.stateJson

    /** Persist a mini-app's state JSON, replacing any prior value. */
    suspend fun setState(id: String, stateJson: String) {
        mutate(id) { it.copy(stateJson = stateJson) }
    }

    private suspend fun mutate(id: String, transform: (MiniApp) -> MiniApp) {
        dataStore.edit { prefs ->
            val current = parse(prefs[KeyMiniApps])
            prefs[KeyMiniApps] = json.encodeToString(
                current.map { if (it.id == id) transform(it) else it },
            )
        }
    }

    private fun parse(raw: String?): List<MiniApp> {
        if (raw.isNullOrEmpty()) return emptyList()
        return runCatching {
            json.decodeFromString<List<MiniApp>>(raw)
        }.getOrDefault(emptyList())
    }

    companion object {
        const val FILE_NAME: String = "saved_features_prefs"
        // Key name `features` kept for back-compat with the old
        // SavedFeaturesRepository — see file-level comment.
        private val KeyMiniApps = stringPreferencesKey("features")
    }
}
