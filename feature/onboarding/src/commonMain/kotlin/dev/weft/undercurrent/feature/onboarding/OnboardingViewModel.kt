package dev.weft.undercurrent.feature.onboarding

import dev.weft.undercurrent.core.domain.OnboardingRepository
import dev.weft.undercurrent.core.navigation.NavigationIntent
import dev.weft.undercurrent.core.navigation.Navigator
import dev.weft.undercurrent.core.navigation.Screen
import dev.weft.undercurrent.shared.mvi.MviViewModel

class OnboardingViewModel(
    private val repo: OnboardingRepository,
    private val navigationVm: Navigator,
) : MviViewModel<Unit, OnboardingIntent, OnboardingEffect>(
    initialState = Unit,
) {
    override fun dispatch(intent: OnboardingIntent) = launch {
        when (intent) {
            OnboardingIntent.CompleteOnboarding -> {
                repo.markCompleted()
                navigationVm.dispatch(NavigationIntent.NavigateAndClear(Screen.KeyPaste))
            }
        }
    }
}

sealed interface OnboardingEffect
