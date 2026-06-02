package dev.weft.undercurrent.core.navigation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable

/**
 * Minimal KMP-friendly replacement for `androidx.navigation3.ui
 * .NavDisplay`. Renders the top entry of [backStack] via
 * [entryProvider] from commonMain — the official UI artifact ships
 * Android + JVM only.
 *
 * Trade-off vs the real `NavDisplay`:
 *
 *   - **Lost**: predictive-back animation, multi-pane
 *     (`SceneStrategy`) support, the `LocalNavAnimatedContentScope`
 *     shared-element hook.
 *   - **Kept**: the back-stack data model + every NavKey-driven
 *     dispatch the rest of the app needs.
 *   - **Restored**: enter/exit transitions, via [AnimatedContent]. The
 *     [transition] resolver lets the host pick a per-destination
 *     animation (e.g. per [NavGraph]); unmapped entries fall back to a
 *     plain cross-fade.
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
    transition: (T) -> ContentTransform? = { null },
    entryProvider: @Composable (T) -> Unit,
) {
    val current = backStack.lastOrNull() ?: return
    AnimatedContent(
        targetState = current,
        transitionSpec = { transition(targetState) ?: DefaultTransition },
        label = "NavDisplay",
    ) { entry ->
        entryProvider(entry)
    }
}

private val DefaultTransition: ContentTransform =
    fadeIn(tween(220)) togetherWith fadeOut(tween(220))
