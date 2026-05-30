package dev.weft.undercurrent.feature.onboarding

/**
 * Onboarding-surface intents. First-run only — once the flag is
 * persisted in the onboarding repo, the boot path skips straight to
 * KeyPaste / Chat and these intents never fire again.
 */
sealed interface OnboardingIntent {

    /**
     * Mark first-run onboarding complete. The handler persists the
     * flag, then resets the nav stack to KeyPaste so the user pastes
     * their first key.
     */
    data object CompleteOnboarding : OnboardingIntent
}
