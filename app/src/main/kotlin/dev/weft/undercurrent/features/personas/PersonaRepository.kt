package dev.weft.undercurrent.features.personas

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.serialization.json.Json
import java.util.UUID

private val Context.personaDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "persona_prefs",
)

/**
 * Persisted persona selection + user-created custom personas.
 *
 * **Two slots**: a voice + an optional role. The user can have both
 * active at once — the runtime's `extraVolatilePrefix` lambda composes
 * voice prompt + role prompt per turn.
 *
 *  - [activeVoice] is always set (defaults to [BuiltInPersonas.Default]
 *    which has an empty prompt = no-op). Selecting any voice / custom
 *    in the picker replaces it.
 *  - [activeRole] is nullable. Tapping a role sets it; tapping the
 *    active role clears it (see [PersonasViewModel] for the toggle
 *    logic; the repo just has setters).
 *
 * **Sync access**: both are [StateFlow]s so the runtime lambda can read
 * `.value.systemPromptText` synchronously per turn without suspending.
 *
 * **Persistence shape**: one preferences file with three keys:
 *  - `active_voice_id` — the selected voice / custom id.
 *  - `active_role_id` — the selected role id (or absent = no role).
 *  - `custom_personas` — JSON-encoded `List<Persona>` of user creations.
 *
 * **Legacy migration**: pre-dual-slot installs stored everything under
 * `active_id`. On first read we silently migrate that value into the
 * appropriate slot (role-kind id → active_role_id; everything else →
 * active_voice_id). The legacy key is left in place (harmless garbage)
 * to avoid touching the prefs file before we've confirmed the new keys
 * land cleanly.
 */
internal class PersonaRepository(context: Context) {
    private val dataStore = context.applicationContext.personaDataStore

    /**
     * Process-scoped supervisor for the StateFlow collectors. Cancelled
     * implicitly when the process dies — we don't expose a `close()`
     * because there's exactly one PersonaRepository per app and its
     * lifetime matches the process.
     */
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val json = Json { ignoreUnknownKeys = true }

    private val activeVoiceIdFlow: Flow<String> = dataStore.data
        .catch { emit(androidx.datastore.preferences.core.emptyPreferences()) }
        .map { prefs ->
            prefs[KeyActiveVoiceId]
                ?: prefs[KeyActiveId]?.takeIf { id ->
                    // Legacy migration: only treat the old id as a voice
                    // if it's NOT a built-in role. Roles are new in this
                    // version so this only matters going forward; for
                    // existing users the legacy id is always voice/custom.
                    BuiltInPersonas.byId(id)?.kind != PersonaKind.Role
                }
                ?: BuiltInPersonas.Default.id
        }

    private val activeRoleIdFlow: Flow<String?> = dataStore.data
        .catch { emit(androidx.datastore.preferences.core.emptyPreferences()) }
        .map { prefs ->
            prefs[KeyActiveRoleId]
                ?: prefs[KeyActiveId]?.takeIf { id ->
                    BuiltInPersonas.byId(id)?.kind == PersonaKind.Role
                }
        }

    private val customPersonasFlow: Flow<List<Persona>> = dataStore.data
        .catch { emit(androidx.datastore.preferences.core.emptyPreferences()) }
        .map { prefs ->
            val raw = prefs[KeyCustomPersonas] ?: return@map emptyList()
            runCatching { json.decodeFromString<List<Persona>>(raw) }.getOrDefault(emptyList())
        }

    val customPersonas: StateFlow<List<Persona>> = customPersonasFlow
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    /**
     * The currently-active voice. Always non-null — when nothing is
     * selected, this is [BuiltInPersonas.Default] (empty prompt = no-op).
     * Filters out Role-kind personas defensively: if the voice slot
     * somehow points at a role (id collision, stale state), we fall
     * back to Default rather than silently swap a role into the voice
     * position.
     */
    val activeVoice: StateFlow<Persona> = combine(activeVoiceIdFlow, customPersonasFlow) { id, customs ->
        BuiltInPersonas.byId(id)?.takeIf { it.kind != PersonaKind.Role }
            ?: customs.firstOrNull { it.id == id && it.kind != PersonaKind.Role }
            ?: BuiltInPersonas.Default
    }.stateIn(scope, SharingStarted.Eagerly, BuiltInPersonas.Default)

    /**
     * The currently-active role, or null when none is selected. Resolves
     * built-in roles AND custom personas tagged [PersonaKind.Role] — so
     * users can add their own roles (e.g. "Pediatrician", "Tax Attorney")
     * via the "+ New" in the ROLES section header.
     */
    val activeRole: StateFlow<Persona?> = combine(activeRoleIdFlow, customPersonasFlow) { id, customs ->
        id?.let {
            BuiltInPersonas.byId(it)?.takeIf { p -> p.kind == PersonaKind.Role }
                ?: customs.firstOrNull { c -> c.id == it && c.kind == PersonaKind.Role }
        }
    }.stateIn(scope, SharingStarted.Eagerly, null)

    /**
     * Set the active voice. Pass any voice or custom id — the picker
     * routes voice + custom taps here. Built-in role ids are rejected at
     * the call site (PersonasViewModel checks kind).
     */
    suspend fun setActiveVoice(id: String) {
        dataStore.edit { it[KeyActiveVoiceId] = id }
    }

    /**
     * Set the active role, or pass null to clear. Tap-to-toggle UX
     * lives in PersonasViewModel.
     */
    suspend fun setActiveRole(id: String?) {
        dataStore.edit { prefs ->
            if (id == null) prefs.remove(KeyActiveRoleId)
            else prefs[KeyActiveRoleId] = id
        }
    }

    /**
     * Add a new custom persona. Returns the created instance (with a
     * generated id). [kind] determines which slot the persona belongs
     * to (Voice / Role); the picker UI passes the right value based on
     * which "+ New" button the user tapped.
     */
    suspend fun addCustom(
        name: String,
        tagline: String,
        systemPromptText: String,
        kind: PersonaKind,
    ): Persona {
        val newPersona = Persona(
            id = "custom.${UUID.randomUUID().toString().take(8)}",
            name = name,
            tagline = tagline,
            systemPromptText = systemPromptText,
            isBuiltIn = false,
            kind = kind,
        )
        dataStore.edit { prefs ->
            val current = parseCustom(prefs[KeyCustomPersonas])
            prefs[KeyCustomPersonas] = json.encodeToString(current + newPersona)
        }
        return newPersona
    }

    /**
     * Update an existing custom persona in place. No-op if [id] doesn't
     * match a stored custom persona.
     */
    suspend fun updateCustom(
        id: String,
        name: String,
        tagline: String,
        systemPromptText: String,
    ) {
        dataStore.edit { prefs ->
            val current = parseCustom(prefs[KeyCustomPersonas])
            val updated = current.map { persona ->
                if (persona.id == id) {
                    persona.copy(
                        name = name,
                        tagline = tagline,
                        systemPromptText = systemPromptText,
                    )
                } else persona
            }
            prefs[KeyCustomPersonas] = json.encodeToString(updated)
        }
    }

    suspend fun deleteCustom(id: String) {
        dataStore.edit { prefs ->
            val current = parseCustom(prefs[KeyCustomPersonas])
            val filtered = current.filterNot { it.id == id }
            prefs[KeyCustomPersonas] = json.encodeToString(filtered)
            // Clear whichever slot(s) referenced the deleted persona.
            // A custom can occupy either slot now (voice OR role), so
            // check both — only one will match, but the writes are
            // idempotent either way.
            if (prefs[KeyActiveVoiceId] == id) {
                prefs[KeyActiveVoiceId] = BuiltInPersonas.Default.id
            }
            if (prefs[KeyActiveRoleId] == id) {
                prefs.remove(KeyActiveRoleId)
            }
            // Legacy: clean up the old single-slot key if it pointed
            // at the deleted custom.
            if (prefs[KeyActiveId] == id) {
                prefs.remove(KeyActiveId)
            }
        }
    }

    private fun parseCustom(raw: String?): List<Persona> {
        if (raw.isNullOrEmpty()) return emptyList()
        return runCatching { json.decodeFromString<List<Persona>>(raw) }.getOrDefault(emptyList())
    }

    private companion object {
        val KeyActiveVoiceId = stringPreferencesKey("active_voice_id")
        val KeyActiveRoleId = stringPreferencesKey("active_role_id")
        val KeyCustomPersonas = stringPreferencesKey("custom_personas")

        /**
         * Legacy single-slot key — read for migration, not written.
         * Pre-dual-slot installs stored everything under this name. Kept
         * to avoid breaking users who haven't opened the app since the
         * change; safe to remove in a future release once we're confident
         * everyone's been migrated.
         */
        val KeyActiveId = stringPreferencesKey("active_id")
    }
}
