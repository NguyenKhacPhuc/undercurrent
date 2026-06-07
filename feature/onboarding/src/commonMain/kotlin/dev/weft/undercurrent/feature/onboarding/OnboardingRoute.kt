package dev.weft.undercurrent.feature.onboarding

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import org.koin.compose.koinInject

@Composable
public fun OnboardingRoute() {
    val onboarding: OnboardingViewModel = koinInject()
    val state by onboarding.state.collectAsState()
    OnboardingScreen(
        modelCountFor = { p -> state.modelCounts[p] ?: 0 },
        onComplete = { picked, _ ->
            onboarding.dispatch(OnboardingIntent.SelectProvider(picked))
            onboarding.dispatch(OnboardingIntent.CompleteOnboarding)
        },
    )
}
