package dev.weft.undercurrent.feature.personas

import dev.weft.undercurrent.core.model.BuiltInPersonas
import dev.weft.undercurrent.core.model.Persona
import dev.weft.undercurrent.core.model.PersonaKind

/**
 * Personas-screen state. Projects [dev.weft.undercurrent.core.domain.PersonaRepository]'s
 * three flows into a single immutable bundle so the screen has one
 * `collectAsState` to observe.
 */
data class PersonasState(
    val activeVoice: Persona = BuiltInPersonas.Default,
    val activeRole: Persona? = null,
    val customPersonas: List<Persona> = emptyList(),
)

/**
 * Intents accepted by [PersonasStore.dispatch]. Fire-and-forget.
 */
sealed interface PersonasIntent {
    /**
     * User tapped a persona card. Voice / Custom kinds switch the
     * voice slot; Role kinds toggle into / out of the role slot
     * (tapping the active role clears it).
     */
    data class TapPersona(val persona: Persona) : PersonasIntent

    /** Save a new custom persona then make it active. */
    data class AddCustom(
        val name: String,
        val tagline: String,
        val systemPromptText: String,
        val kind: PersonaKind,
    ) : PersonasIntent

    /** In-place edit of a custom persona. Doesn't change which slot it's in. */
    data class UpdateCustom(
        val id: String,
        val name: String,
        val tagline: String,
        val systemPromptText: String,
    ) : PersonasIntent

    /** Delete a custom persona. If it was active, repo falls back to Default. */
    data class DeleteCustom(val id: String) : PersonasIntent
}

/**
 * One-shot side effects for Personas. None today — leaving the sealed
 * interface in place so future additions (e.g. "Saved" snackbar) slot
 * in without changing the Store signature.
 */
sealed interface PersonasEffect
