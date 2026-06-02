package dev.weft.undercurrent.feature.providers

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import dev.weft.undercurrent.core.domain.KeyValidationRepository
import dev.weft.undercurrent.core.domain.KeyVaultRepository
import dev.weft.undercurrent.core.domain.ModelCatalogRepository
import dev.weft.undercurrent.core.domain.ModelPrefsRepository
import dev.weft.undercurrent.core.domain.ProviderPrefsRepository
import dev.weft.undercurrent.core.model.ProviderKind
import dev.weft.undercurrent.core.navigation.NavigationIntent
import dev.weft.undercurrent.core.navigation.Navigator
import dev.weft.undercurrent.core.navigation.Screen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.compose.koinInject

@Composable
public fun ProvidersRoute(onOpenConsole: (String) -> Unit) {
    val nav: Navigator = koinInject()
    val provider: ProviderViewModel = koinInject()
    val catalog: ModelCatalogRepository = koinInject()
    val validator: KeyValidationRepository = koinInject()
    val modelPrefs: ModelPrefsRepository = koinInject()
    val providerPrefs: ProviderPrefsRepository = koinInject()
    val keyVault: KeyVaultRepository = koinInject()
    val activeProvider by providerPrefs.activeProvider.collectAsState()
    val defaultTier by providerPrefs.defaultTier.collectAsState()
    var refreshTick by remember { mutableIntStateOf(0) }
    val providerKeyStatus by produceState(emptyMap<ProviderKind, String>(), refreshTick) {
        value = withContext(Dispatchers.Default) {
            buildMap {
                ProviderKind.entries.forEach { p ->
                    if (runCatching { keyVault.hasApiKey(p) }.getOrDefault(false)) {
                        put(p, "•••")
                    }
                }
            }
        }
    }
    ProvidersScreen(
        activeProvider = activeProvider,
        defaultTier = defaultTier,
        providerKeyStatus = providerKeyStatus,
        modelCatalog = catalog,
        keyValidator = validator,
        onProviderSelected = { p -> provider.dispatch(ProviderIntent.SetProvider(p)) },
        onProviderKeySaved = { p, k ->
            provider.dispatch(ProviderIntent.SaveProviderKey(p, k))
            refreshTick++
        },
        onProviderKeyRemoved = { p ->
            provider.dispatch(ProviderIntent.RemoveProviderKey(p))
            refreshTick++
        },
        onDefaultTierSelected = { t -> provider.dispatch(ProviderIntent.SetDefaultTier(t)) },
        getModelOverride = { p, t -> modelPrefs.overrideFor(p, t) },
        onModelOverrideSelected = { p, t, id ->
            provider.dispatch(ProviderIntent.SetModelForTier(p, t, id))
        },
        onOpenConsole = onOpenConsole,
        onBack = { nav.dispatch(NavigationIntent.Navigate(Screen.Settings)) },
    )
}
