package dev.weft.undercurrent.core.domain.usecase.onboarding

import dev.weft.undercurrent.core.domain.OnboardingRepository

/**
 * Mark first-run onboarding complete. The repository's
 * [OnboardingRepository.completedFlow] re-emits afterwards;
 * observers (App boot / OnboardingViewModel) update accordingly.
 */
class CompleteOnboardingUseCase(
    private val repo: OnboardingRepository,
) {
    suspend operator fun invoke() = repo.markCompleted()
}
