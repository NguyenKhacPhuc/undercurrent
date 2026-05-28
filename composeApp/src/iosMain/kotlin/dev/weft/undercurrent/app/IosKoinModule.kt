package dev.weft.undercurrent.app

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import dev.weft.undercurrent.data.datastore.IntegrationsRepository
import dev.weft.undercurrent.data.datastore.MiniAppsRepository
import dev.weft.undercurrent.data.datastore.ModelPrefsRepository
import dev.weft.undercurrent.data.datastore.OnboardingRepository
import dev.weft.undercurrent.data.datastore.PersonaRepository
import dev.weft.undercurrent.data.datastore.ProviderPrefsRepository
import dev.weft.undercurrent.data.datastore.ThemeRepository
import dev.weft.undercurrent.data.datastore.createPreferencesDataStore
import dev.weft.undercurrent.data.sqldelight.DatabaseDriverFactory
import dev.weft.undercurrent.data.sqldelight.createUndercurrentDatabase
import dev.weft.undercurrent.db.UndercurrentDatabase
import dev.weft.undercurrent.feature.conversations.ConversationsViewModel
import dev.weft.undercurrent.feature.creator.CreatorSession
import dev.weft.undercurrent.feature.memories.MemoriesViewModel
import dev.weft.undercurrent.feature.miniapps.MiniAppsViewModel
import dev.weft.undercurrent.feature.personas.PersonasViewModel
import dev.weft.undercurrent.feature.traces.TracesViewModel
import dev.weft.undercurrent.feature.usage.UsageViewModel
import dev.weft.undercurrent.shared.gateway.ConversationStoreGateway
import dev.weft.undercurrent.shared.gateway.KeyValidationGateway
import dev.weft.undercurrent.shared.gateway.KeyVaultGateway
import dev.weft.undercurrent.shared.gateway.MemoryStoreGateway
import dev.weft.undercurrent.shared.gateway.ModelCatalog
import dev.weft.undercurrent.shared.gateway.OAuthGateway
import dev.weft.undercurrent.shared.gateway.SpeechGateway
import dev.weft.undercurrent.shared.gateway.StubConversationStoreGateway
import dev.weft.undercurrent.shared.gateway.StubKeyValidationGateway
import dev.weft.undercurrent.shared.gateway.StubKeyVaultGateway
import dev.weft.undercurrent.shared.gateway.StubMemoryStoreGateway
import dev.weft.undercurrent.shared.gateway.StubModelCatalog
import dev.weft.undercurrent.shared.gateway.StubOAuthGateway
import dev.weft.undercurrent.shared.gateway.StubSpeechGateway
import dev.weft.undercurrent.shared.gateway.StubTraceStoreGateway
import dev.weft.undercurrent.shared.gateway.StubUiBridgeGateway
import dev.weft.undercurrent.shared.gateway.StubUsageGateway
import dev.weft.undercurrent.shared.gateway.TraceStoreGateway
import dev.weft.undercurrent.shared.gateway.UiBridgeGateway
import dev.weft.undercurrent.shared.gateway.UsageGateway
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module

/**
 * iOS Koin module — symmetrical to the Android `appModule` but binds
 * the iOS stub gateways and the iOS DataStore / SQLDelight factories.
 * No `androidContext()`, no `WeftRuntime`, no `AndroidKeyVault`.
 *
 * The 11 stub gateways from `:shared/iosMain` mean every commonMain
 * feature screen resolves its dependencies cleanly — they just see
 * empty flows / no-op writes. Good enough to verify the screen wiring
 * + theming + per-screen Composable correctness on iOS.
 *
 * When a real iOS agent runtime lands, replace `StubXxxGateway` with
 * the real impls (and replace [IosAppStore] with one that drives the
 * agent loop).
 */
public val iosAppModule = module {

    // ─── DataStores ─────────────────────────────────────────────────
    single<DataStore<Preferences>>(named(ThemeRepository.FILE_NAME)) {
        createPreferencesDataStore(name = ThemeRepository.FILE_NAME)
    }
    single<DataStore<Preferences>>(named(OnboardingRepository.FILE_NAME)) {
        createPreferencesDataStore(name = OnboardingRepository.FILE_NAME)
    }
    single<DataStore<Preferences>>(named(PersonaRepository.FILE_NAME)) {
        createPreferencesDataStore(name = PersonaRepository.FILE_NAME)
    }
    single<DataStore<Preferences>>(named(ProviderPrefsRepository.FILE_NAME)) {
        createPreferencesDataStore(name = ProviderPrefsRepository.FILE_NAME)
    }
    single<DataStore<Preferences>>(named(ModelPrefsRepository.FILE_NAME)) {
        createPreferencesDataStore(name = ModelPrefsRepository.FILE_NAME)
    }
    single<DataStore<Preferences>>(named(IntegrationsRepository.FILE_NAME)) {
        createPreferencesDataStore(name = IntegrationsRepository.FILE_NAME)
    }
    single<DataStore<Preferences>>(named(MiniAppsRepository.FILE_NAME)) {
        createPreferencesDataStore(name = MiniAppsRepository.FILE_NAME)
    }

    // ─── Repos ──────────────────────────────────────────────────────
    single { ThemeRepository(get(named(ThemeRepository.FILE_NAME))) }
    single { OnboardingRepository(get(named(OnboardingRepository.FILE_NAME))) }
    single { PersonaRepository(get(named(PersonaRepository.FILE_NAME))) }
    single { ProviderPrefsRepository(get(named(ProviderPrefsRepository.FILE_NAME))) }
    single { ModelPrefsRepository(get(named(ModelPrefsRepository.FILE_NAME))) }
    single { IntegrationsRepository(get(named(IntegrationsRepository.FILE_NAME))) }
    single { MiniAppsRepository(get(named(MiniAppsRepository.FILE_NAME))) }

    // ─── Database ───────────────────────────────────────────────────
    single { DatabaseDriverFactory() }
    single<UndercurrentDatabase> {
        createUndercurrentDatabase(get<DatabaseDriverFactory>().create())
    }

    // ─── Creator-session tracker ────────────────────────────────────
    single { CreatorSession() }

    // ─── Gateways (all stubs) ───────────────────────────────────────
    single<KeyVaultGateway> { StubKeyVaultGateway() }
    single<KeyValidationGateway> { StubKeyValidationGateway() }
    single<OAuthGateway> { StubOAuthGateway() }
    single<ConversationStoreGateway> { StubConversationStoreGateway() }
    single<MemoryStoreGateway> { StubMemoryStoreGateway() }
    single<TraceStoreGateway> { StubTraceStoreGateway() }
    single<UsageGateway> { StubUsageGateway() }
    single<ModelCatalog> { StubModelCatalog() }
    single<SpeechGateway> { StubSpeechGateway() }
    single<UiBridgeGateway> { StubUiBridgeGateway() }

    // ─── AppStore — iOS stub impl ───────────────────────────────────
    single<AppStore> { IosAppStore() }

    // ─── ViewModels ─────────────────────────────────────────────────
    // Same per-screen VMs as Android — they consume the gateway
    // interfaces, so they don't care whether the backing impl is the
    // Weft one or the iOS stub.
    viewModel { PersonasViewModel(repo = get()) }
    viewModel { MiniAppsViewModel(repo = get()) }
    viewModel { UsageViewModel(gateway = get()) }
    viewModel { MemoriesViewModel(store = get()) }
    viewModel { TracesViewModel(store = get()) }
    viewModel { ConversationsViewModel(store = get()) }
    // IntegrationsViewModel needs initialEnabled (snapshot of enabled
    // ids the running runtime was built with). On iOS there's no
    // runtime — pass empty set so the screen renders cleanly without
    // the restart-banner logic firing spuriously.
    viewModel {
        dev.weft.undercurrent.feature.integrations.IntegrationsViewModel(
            repo = get(),
            oauth = get(),
            initialEnabled = emptySet(),
        )
    }
}
