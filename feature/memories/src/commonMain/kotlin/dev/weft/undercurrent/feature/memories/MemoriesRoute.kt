package dev.weft.undercurrent.feature.memories

import androidx.compose.runtime.Composable
import dev.weft.undercurrent.core.navigation.NavigationIntent
import dev.weft.undercurrent.core.navigation.Navigator
import dev.weft.undercurrent.core.navigation.Screen
import org.koin.compose.koinInject

@Composable
public fun MemoriesRoute() {
    val nav: Navigator = koinInject()
    AgentMemoriesScreen(
        onBack = { nav.dispatch(NavigationIntent.Navigate(Screen.Chat)) },
    )
}
