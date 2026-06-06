package dev.weft.undercurrent.feature.onboarding

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import dev.weft.undercurrent.core.domain.KeyValidationRepository
import dev.weft.undercurrent.core.domain.KeyVaultRepository
import dev.weft.undercurrent.core.domain.ProviderPrefsRepository
import dev.weft.undercurrent.feature.settings.providers.ProviderIntent
import dev.weft.undercurrent.feature.settings.providers.ProviderViewModel
import org.koin.compose.koinInject

@Composable
fun KeyPasteRoute(onOpenConsole: (String) -> Unit) {
    val provider: ProviderViewModel = koinInject()
    val validator: KeyValidationRepository = koinInject()
    val keyVault: KeyVaultRepository = koinInject()
    val providerPrefs: ProviderPrefsRepository = koinInject()
    val activeProvider by providerPrefs.activeProvider.collectAsState()
    KeyPasteScreen(
        provider = activeProvider,
        validator = validator,
        onKeyAccepted = { key -> provider.dispatch(ProviderIntent.SubmitKey(key)) },
        saveKey = { key -> keyVault.putApiKey(activeProvider, key) },
        onOpenConsole = onOpenConsole,
    )
}
