package dev.weft.undercurrent.feature.onboarding

import org.koin.dsl.module

val onboardingModule = module {
    single {
        OnboardingViewModel(
            repo = get(),
            navigationVm = get(),
            provider = get(),
            catalog = get(),
        )
    }
    single {
        KeyPasteViewModel(
            validator = get(),
            keyVault = get(),
            providerPrefs = get(),
            provider = get(),
        )
    }
}
