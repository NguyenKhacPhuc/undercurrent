package dev.weft.undercurrent.features.navigation

import dev.weft.undercurrent.core.Screen
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Process-scoped pipe for navigation requests originating *outside* the
 * Compose tree — primarily agent tools (e.g. `open_personas` fired
 * because the user said "I want to switch personas").
 *
 * The flow goes:
 *   1. A `WeftTool` calls [requestNavigate].
 *   2. [dev.weft.undercurrent.core.AppStore] collects from [requests]
 *      and dispatches `AppIntent.Navigate(screen)`, which the reducer
 *      applies the same way a UI tap would.
 *
 * Modeled on the substrate's `OAuthCallbackChannel`: a single shared
 * flow, small buffer to absorb the (rare) race where the tool emits
 * before AppStore's collector resumes. State changes flow back to the
 * agent through the normal screen state — the tool's caller doesn't
 * await navigation completion.
 */
internal class NavigationChannel {

    private val _requests: MutableSharedFlow<Screen> = MutableSharedFlow(
        replay = 0,
        extraBufferCapacity = REQUEST_BUFFER,
    )

    /** Subscribe-only view. AppStore collects from this. */
    val requests: SharedFlow<Screen> = _requests.asSharedFlow()

    /**
     * Request navigation to [screen]. Fire-and-forget — the caller
     * (typically a `WeftTool.executeWeft`) doesn't suspend; the reducer
     * applies the change on its own coroutine.
     */
    fun requestNavigate(screen: Screen) {
        _requests.tryEmit(screen)
    }

    private companion object {
        const val REQUEST_BUFFER = 4
    }
}
