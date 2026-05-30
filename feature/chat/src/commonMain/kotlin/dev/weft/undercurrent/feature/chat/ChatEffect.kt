package dev.weft.undercurrent.feature.chat

import dev.weft.undercurrent.core.domain.PermissionDialogPayload

/**
 * One-shot side effects emitted by [ChatViewModel]. Consumed via a
 * `LaunchedEffect` on the chat surface (or bubbled up to a host
 * snackbar).
 */
sealed interface ChatEffect {

    /** Surface a transient error message via Snackbar / Toast. */
    data class Error(val message: String) : ChatEffect

    /**
     * A tool failed because the OS denied a runtime permission. The
     * host surfaces a "permission needed" dialog with the friendly
     * copy from [payload] and routes the user to system settings.
     * Doesn't replay on config change — that's what effects are for.
     */
    data class PermissionNeeded(val payload: PermissionDialogPayload) : ChatEffect
}
