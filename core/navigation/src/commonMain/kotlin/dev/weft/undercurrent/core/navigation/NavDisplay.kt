package dev.weft.undercurrent.core.navigation

import androidx.compose.runtime.Composable

/**
 * Minimal KMP-friendly replacement for `androidx.navigation3.ui
 * .NavDisplay`. Renders the top entry of [backStack] via
 * [entryProvider] from commonMain — the official UI artifact ships
 * Android + JVM only.
 *
 * Trade-off vs the real `NavDisplay`:
 *
 *   - **Lost**: built-in enter/exit transitions, predictive-back
 *     animation, multi-pane (`SceneStrategy`) support, the
 *     `LocalNavAnimatedContentScope` shared-element hook.
 *   - **Kept**: the back-stack data model + every NavKey-driven
 *     dispatch the rest of the app needs.
 *
 * When the AndroidX/CMP saveable constraint relaxes (see
 * [NavBackStack] for context), swap this file out for the real
 * `navigation3-ui` artifact.
 *
 * Empty back stack is a programmer error — typical hosts seed the
 * stack with at least one entry before rendering. We render nothing
 * in that case rather than throw, so a transient mid-pop state
 * (briefly empty during dispatch) doesn't crash the UI.
 */
@Composable
fun <T : NavKey> NavDisplay(
    backStack: NavBackStack<T>,
    entryProvider: @Composable (T) -> Unit,
) {
    val current = backStack.lastOrNull() ?: return
    entryProvider(current)
}
