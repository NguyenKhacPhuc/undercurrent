package dev.weft.undercurrent.feature.settings.providers

import dev.weft.undercurrent.feature.settings.providers.internal.WeftProviderViewModel
import org.koin.dsl.module

val providerAndroidModule = module {
    single<ProviderViewModel> {
        WeftProviderViewModel(
            context = get(),
            runtime = get(),
            agentSession = get(),
            agentFactory = get(),
            providerPrefsRepo = get(),
            modelPrefsRepo = get(),
            chatVm = get(),
            keyVaultRepo = get(),
            modelCatalog = get(),
            validator = get(),
        )
    }
}
