package dev.weft.undercurrent.feature.onboarding

import dev.weft.undercurrent.core.domain.ModelCatalogRepository
import dev.weft.undercurrent.core.domain.OnboardingRepository
import dev.weft.undercurrent.core.model.ProviderKind
import dev.weft.undercurrent.core.navigation.NavigationIntent
import dev.weft.undercurrent.core.navigation.NavigationViewModel
import dev.weft.undercurrent.core.navigation.Screen
import dev.weft.undercurrent.feature.settings.providers.ProviderIntent
import dev.weft.undercurrent.feature.settings.providers.ProviderViewModel
import dev.weft.undercurrent.shared.mvi.MviViewModel

data class OnboardingState(
    val modelCounts: Map<ProviderKind, Int> = emptyMap(),
)

class OnboardingViewModel(
    private val repo: OnboardingRepository,
    private val navigationVm: NavigationViewModel,
    private val provider: ProviderViewModel,
    catalog: ModelCatalogRepository,
) : MviViewModel<OnboardingState, OnboardingIntent, OnboardingEffect>(
    initialState = OnboardingState(
        modelCounts = ProviderKind.entries.associateWith { catalog.modelsForProvider(it).size },
    ),
) {
    override fun dispatch(intent: OnboardingIntent) = launch {
        when (intent) {
            is OnboardingIntent.SelectProvider ->
                provider.dispatch(ProviderIntent.SetProvider(intent.provider))
            OnboardingIntent.CompleteOnboarding -> {
                repo.markCompleted()
                navigationVm.dispatch(NavigationIntent.NavigateAndClear(Screen.KeyPaste))
            }
        }
    }
}

sealed interface OnboardingEffect
