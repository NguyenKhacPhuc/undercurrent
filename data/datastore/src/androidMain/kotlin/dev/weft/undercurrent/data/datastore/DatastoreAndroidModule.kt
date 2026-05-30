package dev.weft.undercurrent.data.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import dev.weft.undercurrent.core.domain.IntegrationsRepository
import dev.weft.undercurrent.core.domain.MiniAppsRepository
import dev.weft.undercurrent.core.domain.ModelPrefsRepository
import dev.weft.undercurrent.core.domain.OnboardingRepository
import dev.weft.undercurrent.core.domain.PersonaRepository
import dev.weft.undercurrent.core.domain.ProviderPrefsRepository
import dev.weft.undercurrent.core.domain.ThemeRepository
import org.koin.android.ext.koin.androidContext
import org.koin.core.qualifier.named
import org.koin.dsl.module

val datastoreAndroidModule = module {
    single<DataStore<Preferences>>(named(ThemeRepository.FILE_NAME)) {
        createPreferencesDataStore(androidContext(), ThemeRepository.FILE_NAME)
    }
    single<DataStore<Preferences>>(named(OnboardingRepository.FILE_NAME)) {
        createPreferencesDataStore(androidContext(), OnboardingRepository.FILE_NAME)
    }
    single<DataStore<Preferences>>(named(PersonaRepository.FILE_NAME)) {
        createPreferencesDataStore(androidContext(), PersonaRepository.FILE_NAME)
    }
    single<DataStore<Preferences>>(named(ProviderPrefsRepository.FILE_NAME)) {
        createPreferencesDataStore(androidContext(), ProviderPrefsRepository.FILE_NAME)
    }
    single<DataStore<Preferences>>(named(ModelPrefsRepository.FILE_NAME)) {
        createPreferencesDataStore(androidContext(), ModelPrefsRepository.FILE_NAME)
    }
    single<DataStore<Preferences>>(named(IntegrationsRepository.FILE_NAME)) {
        createPreferencesDataStore(androidContext(), IntegrationsRepository.FILE_NAME)
    }
    single<DataStore<Preferences>>(named(MiniAppsRepository.FILE_NAME)) {
        createPreferencesDataStore(androidContext(), MiniAppsRepository.FILE_NAME)
    }
}
