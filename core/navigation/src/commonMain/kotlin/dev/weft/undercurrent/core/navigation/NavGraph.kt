package dev.weft.undercurrent.core.navigation

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith

/**
 * A named sub-flow over [Screen] keys. The back stack stays flat — a
 * graph is a grouping convention on top of it, not a separate render
 * tree (mirrors how real `navigation3` treats graphs as conventions
 * over keys rather than nested hosts).
 *
 * Two jobs:
 *
 *   - **Start / end a flow as a unit.** [NavigationIntent.StartFlow]
 *     pushes [start]; [NavigationIntent.EndFlow] pops every contiguous
 *     entry that belongs to the flow at once.
 *   - **Own the flow's transition.** [NavDisplay] resolves the graph
 *     of the *target* screen via [graphOf] and animates with its
 *     [transition]. Screens not in any graph fall back to a plain
 *     cross-fade.
 *
 * Membership must be unambiguous: each [Screen] belongs to at most one
 * graph, so [graphOf] is deterministic. Screens that are their own
 * standalone destination (Chat, Conversations, …) stay ungrouped.
 */
sealed interface NavGraph {
    val start: Screen
    val screens: List<Screen>

    /**
     * The flow's enter/exit animation. [forward] is `true` for a push
     * (navigating deeper) and `false` for a pop (back) — so directional
     * transitions can mirror themselves and Back doesn't play the
     * forward animation.
     */
    fun transition(forward: Boolean): ContentTransform

    /** First-launch auth + onboarding cascade. Calm cross-fade. */
    data object Onboarding : NavGraph {
        override val start: Screen = Screen.SignIn
        override val screens: List<Screen> =
            listOf(Screen.SignIn, Screen.Onboarding, Screen.KeyPaste)

        // Symmetric cross-fade — direction makes no visual difference.
        override fun transition(forward: Boolean): ContentTransform =
            fadeIn(tween(400)) togetherWith fadeOut(tween(400))
    }

    /** Settings drill-downs. Horizontal slide that follows nav direction. */
    data object SettingsArea : NavGraph {
        override val start: Screen = Screen.Settings
        override val screens: List<Screen> = listOf(
            Screen.Settings,
            Screen.Providers,
            Screen.Appearance,
            Screen.Usage,
            Screen.Personas,
            Screen.Integrations,
        )

        override fun transition(forward: Boolean): ContentTransform =
            if (forward) {
                // Push: new screen slides in from the right, old exits left.
                (slideInHorizontally(tween(300)) { it } + fadeIn(tween(300))) togetherWith
                    (slideOutHorizontally(tween(300)) { -it } + fadeOut(tween(300)))
            } else {
                // Pop (back): previous screen slides back in from the left,
                // current exits to the right — the mirror of the push.
                (slideInHorizontally(tween(300)) { -it } + fadeIn(tween(300))) togetherWith
                    (slideOutHorizontally(tween(300)) { it } + fadeOut(tween(300)))
            }
    }
}

/** The graph [screen] belongs to, or `null` when it's ungrouped. */
fun graphOf(screen: Screen): NavGraph? =
    when (screen) {
        in NavGraph.Onboarding.screens -> NavGraph.Onboarding
        in NavGraph.SettingsArea.screens -> NavGraph.SettingsArea
        else -> null
    }
