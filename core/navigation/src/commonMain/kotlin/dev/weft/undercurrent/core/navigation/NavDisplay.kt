package dev.weft.undercurrent.core.navigation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

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
    transition: (target: T, forward: Boolean) -> ContentTransform? = { _, _ -> null },
    entryProvider: @Composable (T) -> Unit,
) {
    val current = backStack.lastOrNull() ?: return
    // Resolve nav direction so transitions can mirror on Back. The stack
    // growing is a forward push; shrinking (or an in-place replace) is a pop.
    // previousSize lags by one frame — read here, advanced after the entry
    // settles — so the value seen during the transition is the pre-nav size.
    var previousSize by remember { mutableIntStateOf(backStack.size) }
    val forward = backStack.size >= previousSize
    LaunchedEffect(current) { previousSize = backStack.size }

    AnimatedContent(
        targetState = current,
        transitionSpec = { transition(targetState, forward) ?: DefaultTransition },
        label = "NavDisplay",
    ) { entry ->
        entryProvider(entry)
    }
}

private val DefaultTransition: ContentTransform =
    fadeIn(tween(220)) togetherWith fadeOut(tween(220))
