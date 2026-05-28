package dev.weft.undercurrent.feature.personas

import dev.weft.undercurrent.core.model.BuiltInPersonas
import dev.weft.undercurrent.core.model.Persona
import dev.weft.undercurrent.core.model.PersonaKind

/**
 * Personas-screen state. Projects [dev.weft.undercurrent.data.datastore.PersonaRepository]'s
 * three flows into a single immutable bundle so the screen has one
 * `collectAsState` to observe.
 */
public data class PersonasState(
    public val activeVoice: Persona = BuiltInPersonas.Default,
    public val activeRole: Persona? = null,
    public val customPersonas: List<Persona> = emptyList(),
)

/**
 * Intents accepted by [PersonasStore.dispatch]. Fire-and-forget.
 */
public sealed interface PersonasIntent {
    /**
     * User tapped a persona card. Voice / Custom kinds switch the
     * voice slot; Role kinds toggle into / out of the role slot
     * (tapping the active role clears it).
     */
    public data class TapPersona(public val persona: Persona) : PersonasIntent

    /** Save a new custom persona then make it active. */
    public data class AddCustom(
        public val name: String,
        public val tagline: String,
        public val systemPromptText: String,
        public val kind: PersonaKind,
    ) : PersonasIntent

    /** In-place edit of a custom persona. Doesn't change which slot it's in. */
    public data class UpdateCustom(
        public val id: String,
        public val name: String,
        public val tagline: String,
        public val systemPromptText: String,
    ) : PersonasIntent

    /** Delete a custom persona. If it was active, repo falls back to Default. */
    public data class DeleteCustom(public val id: String) : PersonasIntent
}

/**
 * One-shot side effects for Personas. None today — leaving the sealed
 * interface in place so future additions (e.g. "Saved" snackbar) slot
 * in without changing the Store signature.
 */
public sealed interface PersonasEffect
