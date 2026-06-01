package dev.weft.undercurrent.feature.signin

/**
 * UI state for the first-launch sign-in / register flow. Held by
 * [SignInViewModel]; consumed by `SignInScreen` (stateless).
 *
 * - [mode]: which form the user is filling. Toggleable via
 *   [SignInIntent.SwitchMode]; the shared fields ([email], [password])
 *   are preserved across the toggle; [displayName] is only meaningful
 *   in [Mode.Register].
 * - [topError] / [fieldErrors]: errors to render. `topError` is the
 *   generic "above the form" slot (network failure, BE message without
 *   per-field details, "Invalid email or password" on 401). `fieldErrors`
 *   is the per-field slot, populated only when the BE returns
 *   `invalid_request` with a `details` map (e.g. `{"email": "..."}`).
 * - [showSwitchToSignInShortcut]: surfaced by the ViewModel when the BE
 *   answers Register with `email_already_registered`. The UI renders a
 *   one-tap action that dispatches [SignInIntent.SwitchToSignInWithEmail].
 *
 * The in-flight `signUp` / `signIn` signal lives on `MviViewModel.loading`
 * (not here) — every VM gets that for free, and the screen renders a
 * full-screen overlay against it. See `SignInRoute`.
 */
data class SignInState(
    val mode: Mode = Mode.SignIn,
    val email: String = "",
    val password: String = "",
    val displayName: String = "",
    val topError: TopError? = null,
    val fieldErrors: Map<String, String> = emptyMap(),
    val showSwitchToSignInShortcut: Boolean = false,
) {
    enum class Mode { SignIn, Register }

    /** All required fields for the active [mode] are filled and pass client-side validation. */
    val canSubmit: Boolean
        get() = when (mode) {
            Mode.SignIn -> emailLooksValid(email) && password.isNotEmpty()
            Mode.Register ->
                displayNameValid(displayName) &&
                    emailLooksValid(email) &&
                    password.length >= MIN_PASSWORD_LENGTH
        }

    companion object {
        const val MIN_PASSWORD_LENGTH: Int = 8
        const val MAX_DISPLAY_NAME_LENGTH: Int = 40
    }
}

/**
 * Rendered above the form when populated. Subtypes carry whatever extra
 * data the UI needs (e.g. the network case shows a Retry action).
 */
sealed interface TopError {
    /** Generic BE error — [message] is the BE-supplied human string. */
    data class Message(val message: String) : TopError

    /** 429 from sign-in. No countdown per [decisions#D5]. */
    data object RateLimited : TopError

    /** 401 from sign-in. */
    data object InvalidCredentials : TopError

    /** Transport-level failure (timeout, unreachable, no internet). */
    data object Network : TopError
}

/**
 * Loose email check that mirrors the BE's `isLooselyValidEmail` in
 * `backend/.../SignUpRoute.kt`: contains `@`, contains `.` somewhere
 * after `@`, no whitespace. Same client-side as server-side so the
 * user gets fast inline feedback before any submit; the BE is still the
 * ultimate authority.
 */
internal fun emailLooksValid(raw: String): Boolean {
    val value = raw.trim()
    if (value.isEmpty()) return false
    if (value.any { it.isWhitespace() }) return false
    val at = value.indexOf('@')
    if (at <= 0 || at == value.lastIndex) return false
    val dot = value.indexOf('.', startIndex = at + 1)
    return dot != -1 && dot != value.lastIndex
}

/** Display-name validation mirrors the BE: non-empty after trim, ≤ 40 chars. */
internal fun displayNameValid(raw: String): Boolean {
    val trimmed = raw.trim()
    return trimmed.isNotEmpty() && trimmed.length <= SignInState.MAX_DISPLAY_NAME_LENGTH
}
