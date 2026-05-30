package dev.weft.undercurrent.core.domain.usecase.onboarding

import org.koin.dsl.module

val onboardingUseCasesModule = module {
    factory { CompleteOnboardingUseCase(repo = get()) }
}
