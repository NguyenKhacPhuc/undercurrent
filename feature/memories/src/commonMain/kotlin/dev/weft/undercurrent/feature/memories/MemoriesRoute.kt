package dev.weft.undercurrent.feature.memories

import androidx.compose.runtime.Composable
import dev.weft.undercurrent.core.navigation.NavigationIntent
import dev.weft.undercurrent.core.navigation.NavigationViewModel
import dev.weft.undercurrent.core.navigation.Screen
import org.koin.compose.koinInject

@Composable
public fun MemoriesRoute() {
    val nav: NavigationViewModel = koinInject()
    AgentMemoriesScreen(
        onBack = { nav.dispatch(NavigationIntent.Navigate(Screen.Chat)) },
    )
}
