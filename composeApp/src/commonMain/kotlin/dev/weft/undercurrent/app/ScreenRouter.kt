package dev.weft.undercurrent.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import dev.weft.undercurrent.core.model.PersonaKind
import dev.weft.undercurrent.core.navigation.Screen
import dev.weft.undercurrent.core.ui.LoadingPlaceholder
import dev.weft.undercurrent.feature.creator.CreatorKind
import dev.weft.undercurrent.feature.integrations.IntegrationsScreen
import dev.weft.undercurrent.feature.keypaste.KeyPasteScreen
import dev.weft.undercurrent.feature.memories.AgentMemoriesScreen
import dev.weft.undercurrent.feature.onboarding.OnboardingScreen
import dev.weft.undercurrent.feature.personas.PersonasScreen
import dev.weft.undercurrent.feature.providers.ProvidersScreen
import dev.weft.undercurrent.feature.settings.SettingsScreen
import dev.weft.undercurrent.feature.theme.AppearanceScreen
import dev.weft.undercurrent.feature.traces.TraceViewerScreen
import dev.weft.undercurrent.feature.usage.UsageScreen
import dev.weft.undercurrent.core.domain.ModelPrefsRepository
import dev.weft.undercurrent.feature.conversations.ConversationsListScreen
import dev.weft.undercurrent.shared.gateway.KeyValidationGateway
import dev.weft.undercurrent.shared.gateway.ModelCatalog
import org.koin.compose.koinInject

/**
 * Top-level screen switch. KMP-clean screens (Settings, Appearance,
 * Personas, Conversations, Memories, Traces, Usage, Integrations,
 * KeyPaste, Onboarding, Providers) render here directly. Substrate-
 * coupled screens (Chat, RenderedTree, Creator, MiniApps with
 * TreeRenderer) delegate to the [PlatformAdapter].
 */
@Composable
internal fun ScreenRouter(
    state: AppState,
    store: AppStore,
    platform: PlatformAdapter,
) {
    when (state.screen) {
        Screen.Loading -> LoadingPlaceholder()

        Screen.Onboarding -> {
            val modelCatalog: ModelCatalog = koinInject()
            OnboardingScreen(
                modelCountFor = { p -> modelCatalog.modelsForProvider(p).size },
                onComplete = { picked, voiceId ->
                    store.dispatch(AppIntent.SetProvider(picked))
                    store.dispatch(AppIntent.CompleteOnboarding)
                    @Suppress("UNUSED_VARIABLE")
                    val _ignored = voiceId
                },
            )
        }

        Screen.KeyPaste -> {
            val validator: KeyValidationGateway = koinInject()
            KeyPasteScreen(
                provider = state.activeProvider,
                validator = validator,
                onKeyAccepted = { key -> store.dispatch(AppIntent.SubmitKey(key)) },
                saveKey = { key -> store.saveKey(key) },
                onOpenConsole = platform.onOpenUrl,
            )
        }

        Screen.Chat -> {
            if (!state.agentReady) {
                LaunchedEffect(Unit) {
                    store.dispatch(AppIntent.Navigate(Screen.KeyPaste))
                }
            } else {
                platform.chatRoute()
            }
        }

        Screen.RenderedTree -> platform.renderedTreeRoute()

        Screen.Creator -> platform.creatorRoute()

        Screen.MiniApps -> platform.miniAppsRoute()

        Screen.Conversations -> {
            if (!state.agentReady) {
                LaunchedEffect(Unit) {
                    store.dispatch(AppIntent.Navigate(Screen.KeyPaste))
                }
            } else {
                ConversationsListScreen(
                    activeConversationId = state.currentConversationId,
                    onSelect = { id -> store.dispatch(AppIntent.SelectConversation(id)) },
                    onNewChat = { store.dispatch(AppIntent.NewChat) },
                    onBack = { store.dispatch(AppIntent.Navigate(Screen.Chat)) },
                )
            }
        }

        Screen.Traces -> TraceViewerScreen(
            onBack = { store.dispatch(AppIntent.Navigate(Screen.Chat)) },
            onExportTrace = { trace -> store.dispatch(AppIntent.ExportTrace(trace.id)) },
        )

        Screen.Memories -> AgentMemoriesScreen(
            onBack = { store.dispatch(AppIntent.Navigate(Screen.Chat)) },
        )

        Screen.Settings -> SettingsScreen(
            activeProvider = state.activeProvider,
            onShowProvider = { store.dispatch(AppIntent.Navigate(Screen.Providers)) },
            onShowUsage = { store.dispatch(AppIntent.Navigate(Screen.Usage)) },
            onShowIntegrations = { store.dispatch(AppIntent.Navigate(Screen.Integrations)) },
            onBack = { store.dispatch(AppIntent.Navigate(Screen.Chat)) },
        )

        Screen.Integrations -> IntegrationsScreen(
            onBack = { store.dispatch(AppIntent.Navigate(state.previousScreen)) },
            onRestart = platform.onRestartProcess,
        )

        Screen.Providers -> {
            val modelCatalog: ModelCatalog = koinInject()
            val keyValidator: KeyValidationGateway = koinInject()
            val modelPrefsRepo: ModelPrefsRepository = koinInject()
            ProvidersScreen(
                activeProvider = state.activeProvider,
                defaultTier = state.defaultTier,
                providerKeyStatus = state.providerKeyStatus,
                modelCatalog = modelCatalog,
                keyValidator = keyValidator,
                onProviderSelected = { p -> store.dispatch(AppIntent.SetProvider(p)) },
                onProviderKeySaved = { p, k -> store.dispatch(AppIntent.SaveProviderKey(p, k)) },
                onProviderKeyRemoved = { p -> store.dispatch(AppIntent.RemoveProviderKey(p)) },
                onDefaultTierSelected = { t -> store.dispatch(AppIntent.SetDefaultTier(t)) },
                getModelOverride = { p, t -> modelPrefsRepo.overrideFor(p, t) },
                onModelOverrideSelected = { p, t, id ->
                    store.dispatch(AppIntent.SetModelForTier(p, t, id))
                },
                onOpenConsole = platform.onOpenUrl,
                onBack = { store.dispatch(AppIntent.Navigate(Screen.Settings)) },
            )
        }

        Screen.Appearance -> AppearanceScreen(
            selectedPalette = state.themePrefs.palette,
            selectedMode = state.themePrefs.mode,
            onPaletteSelected = { p -> store.dispatch(AppIntent.SetPalette(p)) },
            onModeSelected = { m -> store.dispatch(AppIntent.SetThemeMode(m)) },
            onBack = { store.dispatch(AppIntent.Navigate(Screen.Settings)) },
        )

        Screen.Usage -> UsageScreen(
            onBack = { store.dispatch(AppIntent.Navigate(Screen.Settings)) },
        )

        Screen.Personas -> PersonasScreen(
            onBack = { store.dispatch(AppIntent.Navigate(Screen.Chat)) },
            onStartCreator = { kind ->
                val ck = when (kind) {
                    PersonaKind.Voice -> CreatorKind.PersonaVoice
                    PersonaKind.Role -> CreatorKind.PersonaRole
                    PersonaKind.Custom -> CreatorKind.PersonaVoice
                }
                store.dispatch(AppIntent.StartCreator(ck))
            },
        )
    }
}
