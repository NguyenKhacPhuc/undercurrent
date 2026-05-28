package dev.weft.undercurrent.feature.miniapps

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.weft.undercurrent.core.model.MiniApp
import dev.weft.undercurrent.data.datastore.MiniAppsRepository
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Screen-scoped wrapper around [MiniAppsRepository]. Used by
 * [MiniAppsScreen] (drawer management view) and indirectly by the chat
 * surface via the chip-tap path.
 *
 * KMP — commonMain. Moved from
 * `app/.../features/miniapps/MiniAppsViewModel.kt`. No behavioral
 * changes; package + import paths now follow `:data:datastore`.
 */
public class MiniAppsViewModel(
    private val repo: MiniAppsRepository,
) : ViewModel() {

    public val miniApps: StateFlow<List<MiniApp>> = repo.miniApps

    public fun add(name: String, emoji: String, triggerPrompt: String) {
        viewModelScope.launch { repo.add(name, emoji, triggerPrompt) }
    }

    public fun update(id: String, name: String, emoji: String, triggerPrompt: String) {
        viewModelScope.launch { repo.update(id, name, emoji, triggerPrompt) }
    }

    public fun delete(id: String) {
        viewModelScope.launch { repo.delete(id) }
    }
}
