package dev.weft.undercurrent.feature.settings.usage

import androidx.compose.runtime.Composable
import dev.weft.undercurrent.core.navigation.NavigationIntent
import dev.weft.undercurrent.core.navigation.NavigationViewModel
import dev.weft.undercurrent.core.navigation.Screen
import org.koin.compose.koinInject

@Composable
fun UsageRoute() {
    val nav: NavigationViewModel = koinInject()
    UsageScreen(
        onBack = { nav.dispatch(NavigationIntent.Navigate(Screen.Settings)) },
    )
}
