package dev.weft.undercurrent.feature.miniapps

import androidx.lifecycle.viewModelScope
import dev.weft.undercurrent.core.model.MiniApp
import dev.weft.undercurrent.core.domain.MiniAppsRepository
import dev.weft.undercurrent.shared.mvi.MviViewModel
import kotlinx.coroutines.launch

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
}

sealed interface MiniAppsEffect

class MiniAppsViewModel(
    private val repo: MiniAppsRepository,
) : MviViewModel<MiniAppsState, MiniAppsIntent, MiniAppsEffect>(
    initialState = MiniAppsState(miniApps = repo.miniApps.value),
) {
    init {
        viewModelScope.launch {
            repo.miniApps.collect { m -> update { it.copy(miniApps = m) } }
        }
    }

    override fun dispatch(intent: MiniAppsIntent) {
        when (intent) {
            is MiniAppsIntent.Add -> viewModelScope.launch {
                repo.add(intent.name, intent.emoji, intent.triggerPrompt)
            }
            is MiniAppsIntent.Update -> viewModelScope.launch {
                repo.update(intent.id, intent.name, intent.emoji, intent.triggerPrompt)
            }
            is MiniAppsIntent.Delete -> viewModelScope.launch { repo.delete(intent.id) }
        }
    }
}
