package dev.weft.undercurrent.feature.providers

import dev.weft.undercurrent.feature.providers.internal.WeftProviderViewModel
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
        )
    }
}
