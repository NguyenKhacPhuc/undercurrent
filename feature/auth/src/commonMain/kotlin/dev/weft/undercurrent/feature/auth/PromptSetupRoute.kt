package dev.weft.undercurrent.feature.auth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import org.koin.compose.viewmodel.koinViewModel

/**
 * Stateful wrapper for the cold-start prompt gate. Once the gate reaches
 * [PromptSetupPhase.Ready] (a prompt is cached or freshly fetched), it calls
 * [onReady] to let startup continue — mirroring the sign-in route's
 * `onSignedIn`.
 */
@Composable
fun PromptSetupRoute(onReady: () -> Unit = {}) {
    val viewModel: PromptSetupViewModel = koinViewModel()
    val state by viewModel.state.collectAsState()

    LaunchedEffect(state.phase) {
        if (state.phase == PromptSetupPhase.Ready) onReady()
    }

    PromptSetupScreen(
        state = state,
        onRetry = { viewModel.dispatch(PromptSetupIntent.Retry) },
    )
}
