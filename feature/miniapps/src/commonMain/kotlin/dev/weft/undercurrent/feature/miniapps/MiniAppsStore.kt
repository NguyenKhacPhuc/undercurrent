package dev.weft.undercurrent.feature.miniapps

import androidx.lifecycle.viewModelScope
import dev.weft.undercurrent.core.model.MiniApp
import dev.weft.undercurrent.data.datastore.MiniAppsRepository
import dev.weft.undercurrent.shared.mvi.Store
import kotlinx.coroutines.launch

public data class MiniAppsState(public val miniApps: List<MiniApp> = emptyList())

public sealed interface MiniAppsIntent {
    public data class Add(
        public val name: String,
        public val emoji: String,
        public val triggerPrompt: String,
    ) : MiniAppsIntent
    public data class Update(
        public val id: String,
        public val name: String,
        public val emoji: String,
        public val triggerPrompt: String,
    ) : MiniAppsIntent
    public data class Delete(public val id: String) : MiniAppsIntent
}

public sealed interface MiniAppsEffect

public class MiniAppsStore(
    private val repo: MiniAppsRepository,
) : Store<MiniAppsState, MiniAppsIntent, MiniAppsEffect>(
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
