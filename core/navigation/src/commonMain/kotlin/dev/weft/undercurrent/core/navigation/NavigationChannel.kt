package dev.weft.undercurrent.core.navigation

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Process-scoped pipe for navigation requests originating *outside*
 * the Compose tree — primarily agent tools (e.g. `open_personas`
 * fired because the user said "I want to switch personas").
 *
 * The flow goes:
 *   1. A `WeftTool` (in `:data:weft`) calls [requestNavigate].
 *   2. The host's app store collects from [requests] and dispatches
 *      a Navigate intent, which the reducer applies the same way a
 *      UI tap would.
 *
 * Modeled on the substrate's `OAuthCallbackChannel`: a single shared
 * flow with a small buffer to absorb the (rare) race where the tool
 * emits before the store's collector resumes.
 *
 * KMP — commonMain. Moved from
 * `app/.../features/navigation/NavigationChannel.kt`. No behavior
 * change; visibility lifted from `internal` to `public` so
 * `:data:weft` tools and the host's androidApp module can share it.
 */
class NavigationChannel {

    private val _requests: MutableSharedFlow<Screen> = MutableSharedFlow(
        replay = 0,
        extraBufferCapacity = REQUEST_BUFFER,
    )

    /** Subscribe-only view. The host's app store collects from this. */
    val requests: SharedFlow<Screen> = _requests.asSharedFlow()

    /**
     * Request navigation to [screen]. Fire-and-forget — the caller
     * (typically a `WeftTool.executeWeft`) doesn't suspend; the
     * reducer applies the change on its own coroutine.
     */
    fun requestNavigate(screen: Screen) {
        _requests.tryEmit(screen)
    }

    private companion object {
        private const val REQUEST_BUFFER = 4
    }
}
