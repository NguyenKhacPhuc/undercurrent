package dev.weft.undercurrent.feature.settings.usage

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import dev.weft.undercurrent.core.navigation.NavigationIntent
import dev.weft.undercurrent.core.navigation.NavigationViewModel
import dev.weft.undercurrent.core.navigation.Screen
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun UsageRoute() {
    val nav: NavigationViewModel = koinInject()
    val vm: UsageViewModel = koinViewModel()
    val state by vm.state.collectAsState()
    UsageScreen(
        state = state,
        onBack = { nav.dispatch(NavigationIntent.Navigate(Screen.Settings)) },
    )
}
