package dev.weft.undercurrent.core.domain

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import dev.weft.undercurrent.core.model.BuiltInPersonas
import dev.weft.undercurrent.core.model.Persona
import dev.weft.undercurrent.core.model.PersonaKind
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
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Persisted persona selection + user-created custom personas.
 *
 * Two slots — voice (always set, defaults to BuiltInPersonas.Default
 * which has empty prompt = no-op) and role (nullable). The runtime's
 * `extraVolatilePrefix` lambda composes voice + role prompt per turn.
 *
 * Sync access via [StateFlow] — the runtime reads `.value` without
 * suspending per turn.
 *
 * Persistence shape — single preferences file with three keys:
 *   - `active_voice_id` — selected voice / custom id
 *   - `active_role_id` — selected role id (or absent = no role)
 *   - `custom_personas` — JSON-encoded `List<Persona>` of user creations
 *
 * Legacy migration — pre-dual-slot installs stored everything under
 * `active_id`. On first read we migrate that into the right slot.
 *
 * KMP — commonMain. Moved from
 * `app/.../features/personas/PersonaRepository.kt`. Two adjustments
 * for KMP:
 *   - `java.util.UUID` → `kotlin.uuid.Uuid` (stdlib since Kotlin 2.0.20)
 *   - Android `Context.preferencesDataStore` delegate → constructor-
 *     injected `DataStore<Preferences>`.
 */
@OptIn(ExperimentalUuidApi::class)
class PersonaRepository(
    private val dataStore: DataStore<Preferences>,
) {

    /**
     * Process-scoped supervisor for the StateFlow collectors. Cancelled
     * implicitly when the process dies — we don't expose `close()`
     * because there's one PersonaRepository per app, lifetime = process.
     */
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val json = Json { ignoreUnknownKeys = true }

    private val activeVoiceIdFlow: Flow<String> = dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { prefs ->
            prefs[KeyActiveVoiceId]
                ?: prefs[KeyActiveId]?.takeIf { id ->
                    // Legacy migration: only treat the old id as a voice
                    // if it's NOT a built-in role.
                    BuiltInPersonas.byId(id)?.kind != PersonaKind.Role
                }
                ?: BuiltInPersonas.Default.id
        }

    private val activeRoleIdFlow: Flow<String?> = dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { prefs ->
            prefs[KeyActiveRoleId]
                ?: prefs[KeyActiveId]?.takeIf { id ->
                    BuiltInPersonas.byId(id)?.kind == PersonaKind.Role
                }
        }

    private val customPersonasFlow: Flow<List<Persona>> = dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { prefs ->
            val raw = prefs[KeyCustomPersonas] ?: return@map emptyList()
            runCatching { json.decodeFromString<List<Persona>>(raw) }.getOrDefault(emptyList())
        }

    val customPersonas: StateFlow<List<Persona>> = customPersonasFlow
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    val activeVoice: StateFlow<Persona> = combine(activeVoiceIdFlow, customPersonasFlow) { id, customs ->
        BuiltInPersonas.byId(id)?.takeIf { it.kind != PersonaKind.Role }
            ?: customs.firstOrNull { it.id == id && it.kind != PersonaKind.Role }
            ?: BuiltInPersonas.Default
    }.stateIn(scope, SharingStarted.Eagerly, BuiltInPersonas.Default)

    val activeRole: StateFlow<Persona?> = combine(activeRoleIdFlow, customPersonasFlow) { id, customs ->
        id?.let {
            BuiltInPersonas.byId(it)?.takeIf { p -> p.kind == PersonaKind.Role }
                ?: customs.firstOrNull { c -> c.id == it && c.kind == PersonaKind.Role }
        }
    }.stateIn(scope, SharingStarted.Eagerly, null)

    suspend fun setActiveVoice(id: String) {
        dataStore.edit { it[KeyActiveVoiceId] = id }
    }

    suspend fun setActiveRole(id: String?) {
        dataStore.edit { prefs ->
            if (id == null) prefs.remove(KeyActiveRoleId)
            else prefs[KeyActiveRoleId] = id
        }
    }

    suspend fun addCustom(
        name: String,
        tagline: String,
        systemPromptText: String,
        kind: PersonaKind,
    ): Persona {
        val newPersona = Persona(
            id = "custom.${Uuid.random().toString().take(8)}",
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
            if (prefs[KeyActiveVoiceId] == id) {
                prefs[KeyActiveVoiceId] = BuiltInPersonas.Default.id
            }
            if (prefs[KeyActiveRoleId] == id) {
                prefs.remove(KeyActiveRoleId)
            }
            if (prefs[KeyActiveId] == id) {
                prefs.remove(KeyActiveId)
            }
        }
    }

    private fun parseCustom(raw: String?): List<Persona> {
        if (raw.isNullOrEmpty()) return emptyList()
        return runCatching { json.decodeFromString<List<Persona>>(raw) }.getOrDefault(emptyList())
    }

    companion object {
        const val FILE_NAME: String = "persona_prefs"
        private val KeyActiveVoiceId = stringPreferencesKey("active_voice_id")
        private val KeyActiveRoleId = stringPreferencesKey("active_role_id")
        private val KeyCustomPersonas = stringPreferencesKey("custom_personas")

        /**
         * Legacy single-slot key — read for migration, not written. Kept
         * to avoid breaking users who haven't opened the app since the
         * dual-slot change.
         */
        private val KeyActiveId = stringPreferencesKey("active_id")
    }
}
