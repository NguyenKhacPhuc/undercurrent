package dev.weft.undercurrent.feature.settings

import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val settingsModule = module {
    viewModel { SettingsViewModel(providerPrefs = get()) }
}
