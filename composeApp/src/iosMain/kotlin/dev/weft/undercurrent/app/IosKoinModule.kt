package dev.weft.undercurrent.app

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.network.ktor3.KtorNetworkFetcherFactory
import dev.weft.compose.components.WeftComponentRegistry
import dev.weft.undercurrent.core.ui.components.undercurrentComponents
import dev.weft.undercurrent.core.domain.IntegrationsRepository
import dev.weft.undercurrent.core.domain.MiniAppsRepository
import dev.weft.undercurrent.core.domain.ModelPrefsRepository
import dev.weft.undercurrent.core.domain.OnboardingRepository
import dev.weft.undercurrent.core.domain.PersonaRepository
import dev.weft.undercurrent.core.domain.ProviderPrefsRepository
import dev.weft.undercurrent.core.domain.ThemeRepository
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
import dev.weft.undercurrent.shared.gateway.StubKeyValidationGateway
import dev.weft.undercurrent.shared.gateway.KeychainKeyVaultGateway
import dev.weft.undercurrent.shared.gateway.StubMemoryStoreGateway
import dev.weft.undercurrent.shared.gateway.StubModelCatalog
import dev.weft.undercurrent.shared.gateway.StubOAuthGateway
import dev.weft.undercurrent.shared.gateway.IosSpeechGateway
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
 * the real impls (and replace [IosAppViewModel] with one that drives the
 * agent loop).
 */
val iosAppModule = module {

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

    // ─── Image loading + component palette ──────────────────────────
    //
    // Coil 3 ImageLoader for the WeftComponent palette's SubcomposeAsyncImage
    // call sites (PhotoFrame / Avatar / hero-style components). Ktor 3
    // network fetcher uses the default Darwin engine — same one
    // AnthropicLlmClient + OpenAICompatLlmClient already pull in for the
    // chat path, so no extra HTTP machinery is added.
    single<ImageLoader> {
        ImageLoader.Builder(PlatformContext.INSTANCE)
            .components { add(KtorNetworkFetcherFactory()) }
            .build()
    }

    // WeftComponentRegistry — the ui_render-side counterpart to the
    // Android host's WeftUi.componentRegistry. The Android shell builds
    // this via WeftUi (which is :android-compose-defaults, Android-only);
    // on iOS we construct the registry directly since WeftComponentRegistry
    // itself lives in :android-compose (KMP-published).
    //
    // No consumer wires this into a render path on iOS yet — a future
    // AgentRenderedTreeScreen-equivalent driven by an iOS UiBridge can
    // pick it up from Koin without further plumbing. Binding the palette
    // proves the dependency chain works end-to-end.
    single<WeftComponentRegistry> {
        WeftComponentRegistry(undercurrentComponents(get()))
    }

    // ─── Gateways (all stubs) ───────────────────────────────────────
    single<KeyVaultGateway> { KeychainKeyVaultGateway() }
    single<KeyValidationGateway> { StubKeyValidationGateway() }
    single<OAuthGateway> { StubOAuthGateway() }
    single<ConversationStoreGateway> { IosConversationStoreGateway(get()) }
    single<MemoryStoreGateway> { StubMemoryStoreGateway() }
    single<TraceStoreGateway> { StubTraceStoreGateway() }
    single<UsageGateway> { StubUsageGateway() }
    single<ModelCatalog> { StubModelCatalog() }
    single<SpeechGateway> { IosSpeechGateway() }
    single<UiBridgeGateway> { StubUiBridgeGateway() }

    // ─── AppViewModel — iOS impl ────────────────────────────────────────
    single<AppViewModel> {
        IosAppViewModel(
            keyVault = get(),
            onboardingRepo = get(),
            themeRepo = get(),
            providerPrefsRepo = get(),
            personaRepo = get(),
            db = get(),
        )
    }

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
