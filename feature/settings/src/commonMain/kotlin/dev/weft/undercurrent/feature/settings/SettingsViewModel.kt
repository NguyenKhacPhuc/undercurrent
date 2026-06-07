package dev.weft.undercurrent.feature.settings

import dev.weft.undercurrent.core.domain.ProviderPrefsRepository
import dev.weft.undercurrent.core.model.ProviderKind
import dev.weft.undercurrent.shared.mvi.MviViewModel

data class SettingsState(
    val activeProvider: ProviderKind = ProviderKind.Anthropic,
)

sealed interface SettingsIntent

sealed interface SettingsEffect

class SettingsViewModel(
    providerPrefs: ProviderPrefsRepository,
) : MviViewModel<SettingsState, SettingsIntent, SettingsEffect>(
    initialState = SettingsState(activeProvider = providerPrefs.activeProvider.value),
) {
    init {
        providerPrefs.activeProvider.collectInto { copy(activeProvider = it) }
    }

    override fun dispatch(intent: SettingsIntent) = launch { }
}
