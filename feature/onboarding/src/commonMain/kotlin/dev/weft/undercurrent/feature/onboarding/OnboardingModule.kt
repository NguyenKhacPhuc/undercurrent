package dev.weft.undercurrent.feature.onboarding

import org.koin.dsl.module

val onboardingModule = module {
    single {
        OnboardingViewModel(
            completeOnboarding = get(),
            navigationVm = get(),
        )
    }
}
