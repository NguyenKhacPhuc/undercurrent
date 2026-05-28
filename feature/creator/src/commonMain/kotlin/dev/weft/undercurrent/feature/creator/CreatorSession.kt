package dev.weft.undercurrent.feature.creator

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Tracks whether the user is currently inside a guided creation flow
 * (persona or mini-app) and which kind. Active state drives:
 *
 *  1. The creator-mode preamble injected via the runtime's
 *     `extraVolatilePrefix` lambda — tells the agent to QnA via
 *     ui_render one step at a time.
 *  2. The [CreatorScreen] UI (header label, navigation back-target).
 *  3. Whether the dedicated `create_persona` / `create_mini_app`
 *     tools should be considered in scope (always registered, but
 *     the preamble nudges the agent only when a session is active).
 *
 * Singleton — one active creation at a time. [start] is idempotent;
 * calling it while a different kind is active replaces the kind.
 *
 * KMP — commonMain. Moved from
 * `app/.../features/creator/CreatorSession.kt`.
 */
class CreatorSession {

    private val _state: MutableStateFlow<CreatorKind?> = MutableStateFlow(null)
    val state: StateFlow<CreatorKind?> = _state.asStateFlow()

    fun current(): CreatorKind? = _state.value
    fun isActive(): Boolean = _state.value != null

    fun start(kind: CreatorKind) {
        _state.value = kind
    }

    fun clear() {
        _state.value = null
    }
}

/**
 * What's being created. Drives the kickoff prompt + the agent's
 * preamble + which finalize tool the agent should call.
 */
enum class CreatorKind {
    PersonaVoice,
    PersonaRole,
    MiniApp,
    ;

    val humanLabel: String
        get() = when (this) {
            PersonaVoice -> "voice persona"
            PersonaRole -> "role persona"
            MiniApp -> "mini-app"
        }

    val screenTitle: String
        get() = when (this) {
            PersonaVoice -> "Create voice"
            PersonaRole -> "Create role"
            MiniApp -> "Create mini-app"
        }
}
