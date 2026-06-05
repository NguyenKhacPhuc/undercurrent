package dev.weft.undercurrent.feature.miniapps

import dev.weft.undercurrent.core.model.MiniApp
import dev.weft.undercurrent.core.domain.MiniAppsRepository
import dev.weft.undercurrent.shared.mvi.MviViewModel

data class MiniAppsState(val miniApps: List<MiniApp> = emptyList())

sealed interface MiniAppsIntent {
    data class Add(
        val name: String,
        val emoji: String,
        val triggerPrompt: String,
    ) : MiniAppsIntent
    data class Update(
        val id: String,
        val name: String,
        val emoji: String,
        val triggerPrompt: String,
    ) : MiniAppsIntent
    data class Delete(val id: String) : MiniAppsIntent

    /** Review/change a mini-app's granted actions after first-run consent. */
    data class SetApprovedScopes(val id: String, val scopes: Set<String>) : MiniAppsIntent
}

sealed interface MiniAppsEffect

class MiniAppsViewModel(
    private val repo: MiniAppsRepository,
) : MviViewModel<MiniAppsState, MiniAppsIntent, MiniAppsEffect>(
    initialState = MiniAppsState(miniApps = repo.miniApps.value),
) {
    init {
        repo.miniApps.collectInto { copy(miniApps = it) }
    }

    override fun dispatch(intent: MiniAppsIntent) = launch {
        when (intent) {
            is MiniAppsIntent.Add -> repo.add(intent.name, intent.emoji, intent.triggerPrompt)
            is MiniAppsIntent.Update -> repo.update(
                intent.id, intent.name, intent.emoji, intent.triggerPrompt,
            )
            is MiniAppsIntent.Delete -> repo.delete(intent.id)
            is MiniAppsIntent.SetApprovedScopes -> repo.approveScopes(intent.id, intent.scopes)
        }
    }
}
