package dev.weft.undercurrent.feature.integrations

import androidx.compose.runtime.Composable
import dev.weft.undercurrent.core.navigation.NavigationIntent
import dev.weft.undercurrent.core.navigation.Navigator
import org.koin.compose.koinInject

@Composable
public fun IntegrationsRoute(onRestart: () -> Unit) {
    val nav: Navigator = koinInject()
    IntegrationsScreen(
        onBack = { nav.dispatch(NavigationIntent.Back) },
        onRestart = onRestart,
    )
}
