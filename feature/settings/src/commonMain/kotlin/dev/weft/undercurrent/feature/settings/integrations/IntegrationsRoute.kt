package dev.weft.undercurrent.feature.settings.integrations

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import dev.weft.undercurrent.core.navigation.NavigationIntent
import dev.weft.undercurrent.core.navigation.NavigationViewModel
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@Composable
public fun IntegrationsRoute(onRestart: () -> Unit) {
    val nav: NavigationViewModel = koinInject()
    val vm: IntegrationsViewModel = koinViewModel()
    val state by vm.state.collectAsState()
    IntegrationsScreen(
        state = state,
        onBack = { nav.dispatch(NavigationIntent.Back) },
        onRestart = onRestart,
        onConnect = { vm.dispatch(IntegrationsIntent.Connect(it)) },
        onDisconnect = { vm.dispatch(IntegrationsIntent.Disconnect(it)) },
    )
}
