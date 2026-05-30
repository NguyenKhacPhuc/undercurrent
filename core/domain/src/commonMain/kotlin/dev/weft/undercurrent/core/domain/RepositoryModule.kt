package dev.weft.undercurrent.core.domain

import org.koin.core.qualifier.named
import org.koin.dsl.module

val repositoryModule = module {
    single { ThemeRepository(get(named(ThemeRepository.FILE_NAME))) }
    single { OnboardingRepository(get(named(OnboardingRepository.FILE_NAME))) }
    single { PersonaRepository(get(named(PersonaRepository.FILE_NAME))) }
    single { ProviderPrefsRepository(get(named(ProviderPrefsRepository.FILE_NAME))) }
    single { ModelPrefsRepository(get(named(ModelPrefsRepository.FILE_NAME))) }
    single { IntegrationsRepository(get(named(IntegrationsRepository.FILE_NAME))) }
    single { MiniAppsRepository(get(named(MiniAppsRepository.FILE_NAME))) }
}
