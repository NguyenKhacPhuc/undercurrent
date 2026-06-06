package dev.weft.undercurrent.feature.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import dev.weft.undercurrent.core.domain.ProviderPrefsRepository
import dev.weft.undercurrent.core.navigation.NavigationIntent
import dev.weft.undercurrent.core.navigation.NavigationViewModel
import dev.weft.undercurrent.core.navigation.Screen
import org.koin.compose.koinInject

@Composable
fun SettingsRoute(onSignedOut: () -> Unit = {}) {
    val nav: NavigationViewModel = koinInject()
    val providerPrefs: ProviderPrefsRepository = koinInject()
    val activeProvider by providerPrefs.activeProvider.collectAsState()
    SettingsScreen(
        activeProvider = activeProvider,
        onShowProvider = { nav.dispatch(NavigationIntent.Navigate(Screen.Providers)) },
        onShowUsage = { nav.dispatch(NavigationIntent.Navigate(Screen.Usage)) },
        onShowIntegrations = { nav.dispatch(NavigationIntent.Navigate(Screen.Integrations)) },
        onBack = { nav.dispatch(NavigationIntent.Navigate(Screen.Chat)) },
        onSignedOut = onSignedOut,
    )
}
