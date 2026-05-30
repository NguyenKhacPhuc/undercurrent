package dev.weft.undercurrent.feature.personas

import dev.weft.undercurrent.core.model.PersonaKind
import dev.weft.undercurrent.core.domain.PersonaRepository
import dev.weft.undercurrent.shared.mvi.MviViewModel

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
class PersonasViewModel(
    private val repo: PersonaRepository,
) : MviViewModel<PersonasState, PersonasIntent, PersonasEffect>(
    initialState = PersonasState(
        activeVoice = repo.activeVoice.value,
        activeRole = repo.activeRole.value,
        customPersonas = repo.customPersonas.value,
    ),
) {
    init {
        repo.activeVoice.collectInto { copy(activeVoice = it) }
        repo.activeRole.collectInto { copy(activeRole = it) }
        repo.customPersonas.collectInto { copy(customPersonas = it) }
    }

    override fun dispatch(intent: PersonasIntent) = launch {
        when (intent) {
            is PersonasIntent.TapPersona -> handleTap(intent)
            is PersonasIntent.AddCustom -> handleAdd(intent)
            is PersonasIntent.UpdateCustom -> handleUpdate(intent)
            is PersonasIntent.DeleteCustom -> repo.deleteCustom(intent.id)
        }
    }

    private suspend fun handleTap(intent: PersonasIntent.TapPersona) {
        val persona = intent.persona
        when (persona.kind) {
            PersonaKind.Role -> {
                val currentId = current.activeRole?.id
                val nextId = if (currentId == persona.id) null else persona.id
                repo.setActiveRole(nextId)
            }
            PersonaKind.Voice, PersonaKind.Custom -> repo.setActiveVoice(persona.id)
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
