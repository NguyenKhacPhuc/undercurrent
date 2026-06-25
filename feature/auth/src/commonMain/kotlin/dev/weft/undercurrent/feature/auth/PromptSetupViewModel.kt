package dev.weft.undercurrent.feature.auth

import dev.weft.undercurrent.core.domain.prompt.PromptConfigRepository
import dev.weft.undercurrent.shared.mvi.MviViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first

/** Where the cold-start prompt gate is in its lifecycle. */
enum class PromptSetupPhase { Connecting, Failed, Ready }

data class PromptSetupState(val phase: PromptSetupPhase = PromptSetupPhase.Connecting)

sealed interface PromptSetupIntent {
    /** The user (or an auto-retry) asks to try the fetch again. */
    data object Retry : PromptSetupIntent
}

/** No one-shot effects; the gate UI reacts to [PromptSetupState.phase]. */
sealed interface PromptSetupEffect

/**
 * The cold-start gate (backend-driven-prompt). With no compiled-in fallback,
 * the assistant can't be built until a base prompt exists. This sits just
 * after sign-in: if one is already cached the gate is [Ready] immediately
 * (works offline); otherwise it fetches, showing [Connecting] then [Failed]
 * (couldn't connect — retry) so the user is never dropped into a blank
 * assistant. [Ready] lets startup continue.
 */
class PromptSetupViewModel(
    private val promptConfig: PromptConfigRepository,
) : MviViewModel<PromptSetupState, PromptSetupIntent, PromptSetupEffect>(
    initialState = PromptSetupState(),
) {

    init {
        launch { attempt() }
    }

    override fun dispatch(intent: PromptSetupIntent): Job = launch {
        when (intent) {
            PromptSetupIntent.Retry -> attempt()
        }
    }

    private suspend fun attempt() {
        if (promptConfig.current.first() != null) {
            update { it.copy(phase = PromptSetupPhase.Ready) }
            return
        }
        update { it.copy(phase = PromptSetupPhase.Connecting) }
        promptConfig.refresh()
        val ready = promptConfig.current.first() != null
        update { it.copy(phase = if (ready) PromptSetupPhase.Ready else PromptSetupPhase.Failed) }
    }
}
