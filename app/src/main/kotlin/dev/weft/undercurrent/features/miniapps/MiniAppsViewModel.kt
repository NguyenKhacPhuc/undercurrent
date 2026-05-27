package dev.weft.undercurrent.features.miniapps

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Screen-scoped wrapper around [MiniAppsRepository].
 *
 * Two consumers:
 *  - [MiniAppsScreen] — the drawer-reached management view (rename
 *    / edit / delete, plus the new tree-preview gallery).
 *  - The chat surface, indirectly via [recordUsage] from the chip-tap
 *    path (the actual dispatch happens in
 *    [dev.weft.undercurrent.core.AppStore] so the existing send-message
 *    machinery handles it).
 */
internal class MiniAppsViewModel(
    private val repo: MiniAppsRepository,
) : ViewModel() {
    val miniApps: StateFlow<List<MiniApp>> = repo.miniApps

    fun add(name: String, emoji: String, triggerPrompt: String) {
        viewModelScope.launch { repo.add(name, emoji, triggerPrompt) }
    }

    fun update(id: String, name: String, emoji: String, triggerPrompt: String) {
        viewModelScope.launch { repo.update(id, name, emoji, triggerPrompt) }
    }

    fun delete(id: String) {
        viewModelScope.launch { repo.delete(id) }
    }
}
