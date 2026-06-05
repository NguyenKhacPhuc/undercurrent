package dev.weft.undercurrent.feature.settings.integrations

import androidx.compose.runtime.Composable
import dev.weft.undercurrent.core.navigation.NavigationIntent
import dev.weft.undercurrent.core.navigation.NavigationViewModel
import org.koin.compose.koinInject

@Composable
public fun IntegrationsRoute(onRestart: () -> Unit) {
    val nav: NavigationViewModel = koinInject()
    IntegrationsScreen(
        onBack = { nav.dispatch(NavigationIntent.Back) },
        onRestart = onRestart,
    )
}
