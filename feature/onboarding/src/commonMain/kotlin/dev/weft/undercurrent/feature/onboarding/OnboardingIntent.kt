package dev.weft.undercurrent.feature.onboarding

import dev.weft.undercurrent.core.model.ProviderKind

/**
 * Onboarding-surface intents. First-run only — once the flag is
 * persisted in the onboarding repo, the boot path skips straight to
 * KeyPaste / Chat and these intents never fire again.
 */
sealed interface OnboardingIntent {

    /** Record the provider the user picked during onboarding. */
    data class SelectProvider(val provider: ProviderKind) : OnboardingIntent

    /**
     * Mark first-run onboarding complete. The handler persists the
     * flag, then resets the nav stack to KeyPaste so the user pastes
     * their first key.
     */
    data object CompleteOnboarding : OnboardingIntent
}
