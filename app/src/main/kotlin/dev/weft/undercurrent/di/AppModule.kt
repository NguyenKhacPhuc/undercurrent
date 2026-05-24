package dev.weft.undercurrent.di

import dev.weft.android.WeftRuntime
import dev.weft.compose.ComposeUiBridge
import dev.weft.compose.WeftUi
import dev.weft.security.NetworkPolicy
import dev.weft.undercurrent.core.ASSISTANT_APP_PREAMBLE
import dev.weft.undercurrent.core.AppStore
import dev.weft.undercurrent.data.InMemoryDataSource
import dev.weft.undercurrent.features.onboarding.OnboardingRepository
import dev.weft.undercurrent.features.personas.PersonaRepository
import dev.weft.undercurrent.features.providers.ModelPrefsRepository
import dev.weft.undercurrent.features.providers.ProviderPrefsRepository
import dev.weft.undercurrent.features.theme.SetThemeModeTool
import dev.weft.undercurrent.features.theme.SetThemePaletteTool
import dev.weft.undercurrent.theme.ThemeRepository
import dev.weft.undercurrent.features.conversations.ConversationsViewModel
import dev.weft.undercurrent.features.memories.MemoriesViewModel
import dev.weft.undercurrent.features.personas.PersonasViewModel
import dev.weft.undercurrent.features.traces.TracesViewModel
import dev.weft.undercurrent.features.usage.UsageViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/**
 * Koin module wiring every long-lived dependency in the app.
 *
 * Layering (bottom → top in dependency direction):
 *  1. **Repos** — DataStore-backed, no upstream deps. Pure Android-context
 *     consumers. Cheap to construct.
 *  2. **UI singletons** — [WeftUi] (Compose-side palette + component
 *     registry) and [ComposeUiBridge] (Compose state holder). The bridge
 *     holds Compose `mutableStateOf` fields — they survive process-internal
 *     Activity recreation because Koin keeps the singleton alive.
 *  3. **Runtime** — [WeftRuntime.create] depends on bridge + weftUi +
 *     personaRepo (the `extraVolatilePrefix` lambda reads persona text per
 *     turn). Koin resolves the DAG lazily on first `get()`.
 *  4. **ViewModel** — [AppStore]. Constructor injection via Koin's
 *     `viewModel { }` DSL; `koinViewModel<AppStore>()` in @Composable
 *     handles scoping to the calling ViewModelStoreOwner.
 */
val appModule = module {
    // ─── Repos ─────────────────────────────────────────────────────────
    single { ThemeRepository(androidContext()) }
    single { OnboardingRepository(androidContext()) }
    single { PersonaRepository(androidContext()) }
    single { ProviderPrefsRepository(androidContext()) }
    single { ModelPrefsRepository(androidContext()) }

    // ─── UI singletons ─────────────────────────────────────────────────
    single { WeftUi(androidContext()) }
    single { ComposeUiBridge(componentRegistry = get<WeftUi>().componentRegistry) }

    // ─── Runtime ───────────────────────────────────────────────────────
    // Heavy construction (database, OS capabilities, network client, all
    // the tool registries). Single { } caches the result for the process
    // lifetime — matches the previous Application-scoped lateinit.
    single<WeftRuntime> {
        val personaRepo: PersonaRepository = get()
        val themeRepo: ThemeRepository = get()
        WeftRuntime.create(
            context = androidContext(),
            uiBridge = get<ComposeUiBridge>(),
            appPromptPreamble = ASSISTANT_APP_PREAMBLE,
            dataSources = listOf(
                InMemoryDataSource(name = "notes"),
                InMemoryDataSource(name = "tasks"),
            ),
            networkPolicy = NetworkPolicy.OPEN,
            componentMetadata = get<WeftUi>().components,
            // App-defined agent tools layered on top of the substrate's
            // built-in catalog. Closures over the Koin-resolved
            // ThemeRepository so the LLM can change palette / mode via
            // chat ("make it darker", "try newsprint") and the UI picks
            // the new values up through the same DataStore flow that
            // backs Settings.
            extraToolsFactory = { ctx ->
                listOf(
                    SetThemePaletteTool(ctx, themeRepo),
                    SetThemeModeTool(ctx, themeRepo),
                )
            },
            // Per-turn persona injection. The picker has two independent
            // slots — voice (always set; Default = empty prompt) and
            // role (optional). We compose them as separate labeled
            // sections so the model can tell which axis each block is
            // shaping. Empty when both are no-op.
            extraVolatilePrefix = {
                val voiceText = personaRepo.activeVoice.value.systemPromptText
                val roleText = personaRepo.activeRole.value?.systemPromptText
                buildString {
                    if (voiceText.isNotBlank()) {
                        append("Voice instructions:\n")
                        append(voiceText)
                        append("\n")
                    }
                    if (!roleText.isNullOrBlank()) {
                        if (isNotEmpty()) append("\n")
                        append("Role instructions:\n")
                        append(roleText)
                        append("\n")
                    }
                }
            },
        )
    }

    // ─── ViewModels ────────────────────────────────────────────────────
    // Root MVI store — global app state (navigation, agent, chat).
    viewModel {
        AppStore(
            runtime = get(),
            themeRepo = get(),
            onboardingRepo = get(),
            providerPrefsRepo = get(),
            modelPrefsRepo = get(),
        )
    }
    // Per-screen VMs — own the dependency for one surface so the screen
    // resolves via `koinViewModel()` instead of receiving stores via
    // MainActivity prop-drilling. Scoped to whichever ViewModelStoreOwner
    // the screen composes under (in practice: MainActivity), so they
    // share lifetime with the activity.
    viewModel { PersonasViewModel(repo = get()) }
    viewModel { UsageViewModel(runtime = get()) }
    viewModel { MemoriesViewModel(runtime = get()) }
    viewModel { TracesViewModel(runtime = get()) }
    viewModel { ConversationsViewModel(runtime = get()) }
}
