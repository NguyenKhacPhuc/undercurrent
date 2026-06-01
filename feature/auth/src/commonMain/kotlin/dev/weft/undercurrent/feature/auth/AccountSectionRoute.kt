package dev.weft.undercurrent.feature.auth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AccountSectionRoute(
    onSignedOut: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val viewModel: AccountViewModel = koinViewModel()
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                AccountEffect.SignedOut -> onSignedOut()
            }
        }
    }

    AccountSection(
        state = state,
        onIntent = { viewModel.dispatch(it) },
        modifier = modifier,
    )
}
