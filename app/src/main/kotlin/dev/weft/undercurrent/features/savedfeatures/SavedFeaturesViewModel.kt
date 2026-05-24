package dev.weft.undercurrent.features.savedfeatures

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Screen-scoped wrapper around [SavedFeaturesRepository].
 *
 * Two consumers:
 *  - [dev.weft.undercurrent.features.savedfeatures.SavedFeaturesScreen]
 *    — the drawer-reached management view (rename / edit / delete).
 *  - The chat surface, indirectly via [recordUsage] from the chip-tap
 *    path (the actual dispatch happens in [dev.weft.undercurrent.core.AppStore]
 *    so the existing send-message machinery handles it).
 */
internal class SavedFeaturesViewModel(
    private val repo: SavedFeaturesRepository,
) : ViewModel() {
    val features: StateFlow<List<SavedFeature>> = repo.features

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
