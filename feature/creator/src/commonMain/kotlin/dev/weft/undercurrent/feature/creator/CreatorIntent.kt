package dev.weft.undercurrent.feature.creator

/**
 * Creator-flow intents — start / cancel a guided creator session.
 * The flow itself is driven by the agent via `ui_render`, so the
 * surface here is small: kick it off and bail out.
 */
sealed interface CreatorIntent {

    /** Begin a guided creator flow. */
    data class StartCreator(val kind: CreatorKind) : CreatorIntent

    /**
     * Cancel the in-flight creator session and route back to its
     * origin (Personas / MiniApps / Settings depending on
     * [CreatorKind]).
     */
    data object CancelCreator : CreatorIntent
}
