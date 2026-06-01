package dev.weft.undercurrent.feature.auth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SignInRoute(onSignedIn: () -> Unit = {}) {
    val viewModel: SignInViewModel = koinViewModel()
    val state by viewModel.state.collectAsState()
    val loading by viewModel.loading.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                SignInEffect.SignedIn -> onSignedIn()
            }
        }
    }

    SignInScreen(
        state = state,
        loading = loading,
        onIntent = { viewModel.dispatch(it) },
    )
}
