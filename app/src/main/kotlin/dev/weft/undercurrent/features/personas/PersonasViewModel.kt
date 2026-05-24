package dev.weft.undercurrent.features.personas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Screen-scoped state for [PersonasScreen]. Wraps [PersonaRepository]
 * so the screen doesn't reach into a global repo via
 * [dev.weft.undercurrent.core.AppStore].
 *
 * Exposes the two active slots ([activeVoice] + [activeRole]) and
 * routes user taps to the correct one based on the persona's
 * [PersonaKind]. Voices and customs go into the voice slot; roles go
 * into the role slot (with tap-to-toggle behavior — tapping the active
 * role clears it).
 */
internal class PersonasViewModel(
    private val repo: PersonaRepository,
) : ViewModel() {
    val activeVoice: StateFlow<Persona> = repo.activeVoice
    val activeRole: StateFlow<Persona?> = repo.activeRole
    val customPersonas: StateFlow<List<Persona>> = repo.customPersonas

    /**
     * Tap handler for any persona row. Routes by kind:
     *  - Voice / Custom → set as active voice (replaces whatever was there)
     *  - Role → toggle. Set if different from current; clear if same.
     */
    fun onPersonaTap(persona: Persona) {
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

    /**
     * Persist a new custom persona and switch to it immediately so the
     * user sees their freshly-created persona become active without an
     * extra tap. [kind] determines which slot the new persona occupies
     * (Voice or Role) — passed by the picker based on which "+ New"
     * the user tapped.
     */
    fun addCustom(name: String, tagline: String, systemPromptText: String, kind: PersonaKind) {
        viewModelScope.launch {
            val created = repo.addCustom(
                name = name,
                tagline = tagline,
                systemPromptText = systemPromptText,
                kind = kind,
            )
            // Auto-activate in the matching slot.
            when (kind) {
                PersonaKind.Role -> repo.setActiveRole(created.id)
                PersonaKind.Voice, PersonaKind.Custom -> repo.setActiveVoice(created.id)
            }
        }
    }

    fun updateCustom(id: String, name: String, tagline: String, systemPromptText: String) {
        viewModelScope.launch {
            repo.updateCustom(id, name, tagline, systemPromptText)
        }
    }

    fun deleteCustom(id: String) {
        viewModelScope.launch { repo.deleteCustom(id) }
    }
}
