package dev.weft.undercurrent.feature.onboarding

import androidx.compose.runtime.Composable
import dev.weft.undercurrent.core.domain.ModelCatalogRepository
import dev.weft.undercurrent.feature.settings.providers.ProviderIntent
import dev.weft.undercurrent.feature.settings.providers.ProviderViewModel
import org.koin.compose.koinInject

@Composable
public fun OnboardingRoute() {
    val provider: ProviderViewModel = koinInject()
    val onboarding: OnboardingViewModel = koinInject()
    val catalog: ModelCatalogRepository = koinInject()
    OnboardingScreen(
        modelCountFor = { p -> catalog.modelsForProvider(p).size },
        onComplete = { picked, _ ->
            provider.dispatch(ProviderIntent.SetProvider(picked))
            onboarding.dispatch(OnboardingIntent.CompleteOnboarding)
        },
    )
}
