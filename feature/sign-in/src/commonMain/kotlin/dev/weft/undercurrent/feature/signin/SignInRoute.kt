package dev.weft.undercurrent.feature.signin

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import org.koin.compose.koinInject

/**
 * Stateful entry point for the first-launch sign-in / register flow.
 * Injects [SignInViewModel], collects [SignInState], and turns
 * [SignInEffect.SignedIn] into the [onSignedIn] callback supplied by
 * the host. ScreenRouter wires `onSignedIn` to `AppViewModel.resume()`
 * so the boot cascade rolls forward to the existing provider/key
 * onboarding step (per `decisions#D7`).
 */
@Composable
fun SignInRoute(onSignedIn: () -> Unit = {}) {
    val vm: SignInViewModel = koinInject()
    val state by vm.state.collectAsState()

    LaunchedEffect(Unit) {
        vm.effects.collect { effect ->
            when (effect) {
                SignInEffect.SignedIn -> onSignedIn()
            }
        }
    }

    SignInScreen(
        state = state,
        onIntent = { vm.dispatch(it) },
    )
}
