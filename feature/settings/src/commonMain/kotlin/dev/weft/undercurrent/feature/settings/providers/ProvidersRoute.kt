package dev.weft.undercurrent.feature.settings.providers

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import dev.weft.undercurrent.core.navigation.NavigationIntent
import dev.weft.undercurrent.core.navigation.NavigationViewModel
import dev.weft.undercurrent.core.navigation.Screen
import org.koin.compose.koinInject

@Composable
public fun ProvidersRoute(onOpenConsole: (String) -> Unit) {
    val nav: NavigationViewModel = koinInject()
    val provider: ProviderViewModel = koinInject()
    val state by provider.state.collectAsState()
    ProvidersScreen(
        state = state,
        onProviderSelected = { p ->
            provider.dispatch(ProviderIntent.SetProvider(p))
            provider.dispatch(ProviderIntent.ClearKeyValidation)
        },
        onSaveKey = { p, k -> provider.dispatch(ProviderIntent.ValidateAndSaveProviderKey(p, k)) },
        onKeyInputChanged = { provider.dispatch(ProviderIntent.ClearKeyValidation) },
        onProviderKeyRemoved = { p -> provider.dispatch(ProviderIntent.RemoveProviderKey(p)) },
        onDefaultTierSelected = { t -> provider.dispatch(ProviderIntent.SetDefaultTier(t)) },
        onModelOverrideSelected = { p, t, id ->
            provider.dispatch(ProviderIntent.SetModelForTier(p, t, id))
        },
        onOpenConsole = onOpenConsole,
        onBack = { nav.dispatch(NavigationIntent.Navigate(Screen.Settings)) },
    )
}
