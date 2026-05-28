package dev.weft.undercurrent.feature.personas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.weft.undercurrent.core.model.Persona
import dev.weft.undercurrent.core.model.PersonaKind
import dev.weft.undercurrent.data.datastore.PersonaRepository
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Screen-scoped state for [PersonasScreen]. Wraps [PersonaRepository]
 * so the screen doesn't reach into a global repo via the root app
 * store.
 *
 * Exposes the two active slots ([activeVoice] + [activeRole]) and
 * routes user taps to the correct one based on the persona's
 * [PersonaKind]. Voices and customs go into the voice slot; roles go
 * into the role slot (with tap-to-toggle — tapping the active role
 * clears it).
 *
 * KMP — commonMain. Moved from
 * `app/.../features/personas/PersonasViewModel.kt`. No behavioral
 * changes; just the package + imports follow the migrated
 * `:data:datastore` PersonaRepository.
 */
public class PersonasViewModel(
    private val repo: PersonaRepository,
) : ViewModel() {

    public val activeVoice: StateFlow<Persona> = repo.activeVoice
    public val activeRole: StateFlow<Persona?> = repo.activeRole
    public val customPersonas: StateFlow<List<Persona>> = repo.customPersonas

    public fun onPersonaTap(persona: Persona) {
        when (persona.kind) {
            PersonaKind.Role -> {
                val current = activeRole.value?.id
                val nextId = if (current == persona.id) null else persona.id
                viewModelScope.launch { repo.setActiveRole(nextId) }
            }
            PersonaKind.Voice, PersonaKind.Custom -> {
                viewModelScope.launch { repo.setActiveVoice(persona.id) }
            }
        }
    }

    public fun addCustom(name: String, tagline: String, systemPromptText: String, kind: PersonaKind) {
        viewModelScope.launch {
            val created = repo.addCustom(
                name = name,
                tagline = tagline,
                systemPromptText = systemPromptText,
                kind = kind,
            )
            when (kind) {
                PersonaKind.Role -> repo.setActiveRole(created.id)
                PersonaKind.Voice, PersonaKind.Custom -> repo.setActiveVoice(created.id)
            }
        }
    }

    public fun updateCustom(id: String, name: String, tagline: String, systemPromptText: String) {
        viewModelScope.launch {
            repo.updateCustom(id, name, tagline, systemPromptText)
        }
    }

    public fun deleteCustom(id: String) {
        viewModelScope.launch { repo.deleteCustom(id) }
    }
}
