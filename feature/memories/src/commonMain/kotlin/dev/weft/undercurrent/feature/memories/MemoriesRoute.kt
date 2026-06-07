package dev.weft.undercurrent.feature.memories

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import dev.weft.undercurrent.core.navigation.NavigationIntent
import dev.weft.undercurrent.core.navigation.NavigationViewModel
import dev.weft.undercurrent.core.navigation.Screen
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@Composable
public fun MemoriesRoute() {
    val nav: NavigationViewModel = koinInject()
    val vm: MemoriesViewModel = koinViewModel()
    val state by vm.state.collectAsState()
    AgentMemoriesScreen(
        state = state,
        onBack = { nav.dispatch(NavigationIntent.Navigate(Screen.Chat)) },
        onDelete = { vm.dispatch(MemoriesIntent.Delete(it)) },
        onClearAll = { vm.dispatch(MemoriesIntent.ClearAll) },
    )
}
