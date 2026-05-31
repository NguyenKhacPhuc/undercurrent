package dev.weft.undercurrent.feature.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import dev.weft.undercurrent.core.navigation.NavigationIntent
import dev.weft.undercurrent.core.navigation.NavigationViewModel
import dev.weft.undercurrent.core.navigation.Screen
import org.koin.compose.koinInject

@Composable
public fun AppearanceRoute() {
    val nav: NavigationViewModel = koinInject()
    val theme: ThemeViewModel = koinInject()
    val state by theme.state.collectAsState()
    AppearanceScreen(
        selectedPalette = state.prefs.palette,
        selectedMode = state.prefs.mode,
        onPaletteSelected = { theme.dispatch(ThemeIntent.SetPalette(it)) },
        onModeSelected = { theme.dispatch(ThemeIntent.SetThemeMode(it)) },
        onBack = { nav.dispatch(NavigationIntent.Navigate(Screen.Settings)) },
    )
}
