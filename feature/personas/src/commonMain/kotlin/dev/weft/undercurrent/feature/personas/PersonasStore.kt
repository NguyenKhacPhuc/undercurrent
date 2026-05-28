package dev.weft.undercurrent.feature.personas

import androidx.lifecycle.viewModelScope
import dev.weft.undercurrent.core.model.PersonaKind
import dev.weft.undercurrent.data.datastore.PersonaRepository
import dev.weft.undercurrent.shared.mvi.Store
import kotlinx.coroutines.launch

/**
 * Screen-scoped MVI store for [PersonasScreen].
 *
 * State source of truth is still [PersonaRepository] (DataStore-backed);
 * the store subscribes to its three flows in [init] and projects them
 * into the single [PersonasState] the screen observes. Every mutation
 * goes through [dispatch] / [PersonasIntent].
 *
 * No effects yet — the sealed [PersonasEffect] type is reserved for
 * future additions (e.g. "Saved" snackbar after add-custom).
 *
 * KMP — commonMain.
 */
public class PersonasStore(
    private val repo: PersonaRepository,
) : Store<PersonasState, PersonasIntent, PersonasEffect>(
    initialState = PersonasState(
        activeVoice = repo.activeVoice.value,
        activeRole = repo.activeRole.value,
        customPersonas = repo.customPersonas.value,
    ),
) {
    init {
        viewModelScope.launch {
            repo.activeVoice.collect { v -> update { it.copy(activeVoice = v) } }
        }
        viewModelScope.launch {
            repo.activeRole.collect { r -> update { it.copy(activeRole = r) } }
        }
        viewModelScope.launch {
            repo.customPersonas.collect { c -> update { it.copy(customPersonas = c) } }
        }
    }

    override fun dispatch(intent: PersonasIntent) {
        when (intent) {
            is PersonasIntent.TapPersona -> handleTap(intent)
            is PersonasIntent.AddCustom -> viewModelScope.launch { handleAdd(intent) }
            is PersonasIntent.UpdateCustom -> viewModelScope.launch { handleUpdate(intent) }
            is PersonasIntent.DeleteCustom -> viewModelScope.launch {
                repo.deleteCustom(intent.id)
            }
        }
    }

    /**
     * Voice / Custom kinds replace the voice slot. Role kinds toggle
     * — tapping the active role clears it.
     */
    private fun handleTap(intent: PersonasIntent.TapPersona) {
        val persona = intent.persona
        when (persona.kind) {
            PersonaKind.Role -> {
                val currentId = current.activeRole?.id
                val nextId = if (currentId == persona.id) null else persona.id
                viewModelScope.launch { repo.setActiveRole(nextId) }
            }
            PersonaKind.Voice, PersonaKind.Custom -> {
                viewModelScope.launch { repo.setActiveVoice(persona.id) }
            }
        }
    }

    private suspend fun handleAdd(intent: PersonasIntent.AddCustom) {
        val created = repo.addCustom(
            name = intent.name,
            tagline = intent.tagline,
            systemPromptText = intent.systemPromptText,
            kind = intent.kind,
        )
        when (intent.kind) {
            PersonaKind.Role -> repo.setActiveRole(created.id)
            PersonaKind.Voice, PersonaKind.Custom -> repo.setActiveVoice(created.id)
        }
    }

    private suspend fun handleUpdate(intent: PersonasIntent.UpdateCustom) {
        repo.updateCustom(
            id = intent.id,
            name = intent.name,
            tagline = intent.tagline,
            systemPromptText = intent.systemPromptText,
        )
    }
}
