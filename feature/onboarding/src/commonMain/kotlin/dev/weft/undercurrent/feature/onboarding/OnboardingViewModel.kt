package dev.weft.undercurrent.feature.onboarding

import dev.weft.undercurrent.core.domain.usecase.onboarding.CompleteOnboardingUseCase
import dev.weft.undercurrent.core.navigation.NavigationIntent
import dev.weft.undercurrent.core.navigation.NavigationViewModel
import dev.weft.undercurrent.core.navigation.Screen
import dev.weft.undercurrent.shared.mvi.MviViewModel

/**
 * Onboarding ViewModel. Minimal — one intent, dispatches it through
 * a UseCase, then triggers the nav transition to KeyPaste.
 *
 * Navigation lives in the VM (not deferred to the screen via an
 * effect) because it's tightly bound to the operation: "completing
 * onboarding" semantically includes "go to KeyPaste". Separating
 * them would force every caller to wire the side effect manually.
 */
class OnboardingViewModel(
    private val completeOnboarding: CompleteOnboardingUseCase,
    private val navigationVm: NavigationViewModel,
) : MviViewModel<Unit, OnboardingIntent, OnboardingEffect>(
    initialState = Unit,
) {
    override fun dispatch(intent: OnboardingIntent) = launch {
        when (intent) {
            OnboardingIntent.CompleteOnboarding -> {
                completeOnboarding()
                navigationVm.dispatch(NavigationIntent.NavigateAndClear(Screen.KeyPaste))
            }
        }
    }
}

/** Effects (empty — no one-shot signals yet). */
sealed interface OnboardingEffect
