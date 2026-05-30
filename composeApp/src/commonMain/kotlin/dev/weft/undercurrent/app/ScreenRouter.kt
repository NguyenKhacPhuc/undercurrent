package dev.weft.undercurrent.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import dev.weft.undercurrent.core.domain.ModelPrefsRepository
import dev.weft.undercurrent.core.model.AppState
import dev.weft.undercurrent.core.model.PersonaKind
import dev.weft.undercurrent.core.navigation.NavDisplay
import dev.weft.undercurrent.core.navigation.NavigationIntent
import dev.weft.undercurrent.core.navigation.NavigationViewModel
import dev.weft.undercurrent.core.navigation.Screen
import dev.weft.undercurrent.core.ui.LoadingPlaceholder
import dev.weft.undercurrent.feature.chat.ChatIntent
import dev.weft.undercurrent.feature.chat.ChatViewModel
import dev.weft.undercurrent.feature.conversations.ConversationsListScreen
import dev.weft.undercurrent.feature.creator.CreatorIntent
import dev.weft.undercurrent.feature.creator.CreatorKind
import dev.weft.undercurrent.feature.creator.CreatorViewModel
import dev.weft.undercurrent.feature.integrations.IntegrationsScreen
import dev.weft.undercurrent.feature.keypaste.KeyPasteScreen
import dev.weft.undercurrent.feature.memories.AgentMemoriesScreen
import dev.weft.undercurrent.feature.onboarding.OnboardingIntent
import dev.weft.undercurrent.feature.onboarding.OnboardingScreen
import dev.weft.undercurrent.feature.onboarding.OnboardingViewModel
import dev.weft.undercurrent.feature.personas.PersonasScreen
import dev.weft.undercurrent.feature.providers.ProviderIntent
import dev.weft.undercurrent.feature.providers.ProviderViewModel
import dev.weft.undercurrent.feature.providers.ProvidersScreen
import dev.weft.undercurrent.feature.settings.SettingsScreen
import dev.weft.undercurrent.feature.theme.AppearanceScreen
import dev.weft.undercurrent.feature.theme.ThemeIntent
import dev.weft.undercurrent.feature.theme.ThemeViewModel
import dev.weft.undercurrent.feature.traces.TraceExportViewModel
import dev.weft.undercurrent.feature.traces.TraceIntent
import dev.weft.undercurrent.feature.traces.TraceViewerScreen
import dev.weft.undercurrent.feature.usage.UsageScreen
import dev.weft.undercurrent.core.domain.KeyValidationRepository
import dev.weft.undercurrent.core.domain.ModelCatalogRepository
import org.koin.compose.koinInject

@Composable
internal fun ScreenRouter(
    state: AppState,
    store: AppViewModel,
    platform: PlatformAdapter,
) {
    val navigationVm: NavigationViewModel = koinInject()
    val chatVm: ChatViewModel = koinInject()
    val themeVm: ThemeViewModel = koinInject()
    val onboardingVm: OnboardingViewModel = koinInject()
    val providerVm: ProviderViewModel = koinInject()
    val creatorVm: CreatorViewModel = koinInject()
    val traceExportVm: TraceExportViewModel = koinInject()
    NavDisplay(backStack = navigationVm.backStack) { entry ->
        when (entry) {
            Screen.Loading -> LoadingPlaceholder()

            Screen.Onboarding -> {
                val modelCatalog: ModelCatalogRepository = koinInject()
                OnboardingScreen(
                    modelCountFor = { p -> modelCatalog.modelsForProvider(p).size },
                    onComplete = { picked, voiceId ->
                        providerVm.dispatch(ProviderIntent.SetProvider(picked))
                        onboardingVm.dispatch(OnboardingIntent.CompleteOnboarding)
                    },
                )
            }

            Screen.KeyPaste -> {
                val validator: KeyValidationRepository = koinInject()
                KeyPasteScreen(
                    provider = state.activeProvider,
                    validator = validator,
                    onKeyAccepted = { key -> providerVm.dispatch(ProviderIntent.SubmitKey(key)) },
                    saveKey = { key -> store.saveKey(key) },
                    onOpenConsole = platform.onOpenUrl,
                )
            }

            Screen.Chat -> {
                if (!state.agentReady) {
                    LaunchedEffect(Unit) {
                        navigationVm.dispatch(NavigationIntent.Navigate(Screen.KeyPaste))
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
                        navigationVm.dispatch(NavigationIntent.Navigate(Screen.KeyPaste))
                    }
                } else {
                    ConversationsListScreen(
                        activeConversationId = state.currentConversationId,
                        onSelect = { id -> chatVm.dispatch(ChatIntent.SelectConversation(id)) },
                        onNewChat = { chatVm.dispatch(ChatIntent.NewChat) },
                        onBack = { navigationVm.dispatch(NavigationIntent.Navigate(Screen.Chat)) },
                    )
                }
            }

            Screen.Traces -> TraceViewerScreen(
                onBack = { navigationVm.dispatch(NavigationIntent.Navigate(Screen.Chat)) },
                onExportTrace = { trace -> traceExportVm.dispatch(TraceIntent.ExportTrace(trace.id)) },
            )

            Screen.Memories -> AgentMemoriesScreen(
                onBack = { navigationVm.dispatch(NavigationIntent.Navigate(Screen.Chat)) },
            )

            Screen.Settings -> SettingsScreen(
                activeProvider = state.activeProvider,
                onShowProvider = { navigationVm.dispatch(NavigationIntent.Navigate(Screen.Providers)) },
                onShowUsage = { navigationVm.dispatch(NavigationIntent.Navigate(Screen.Usage)) },
                onShowIntegrations = {
                    navigationVm.dispatch(NavigationIntent.Navigate(Screen.Integrations))
                },
                onBack = { navigationVm.dispatch(NavigationIntent.Navigate(Screen.Chat)) },
            )

            Screen.Integrations -> IntegrationsScreen(
                onBack = { navigationVm.dispatch(NavigationIntent.Back) },
                onRestart = platform.onRestartProcess,
            )

            Screen.Providers -> {
                val modelCatalog: ModelCatalogRepository = koinInject()
                val keyValidator: KeyValidationRepository = koinInject()
                val modelPrefsRepo: ModelPrefsRepository = koinInject()
                ProvidersScreen(
                    activeProvider = state.activeProvider,
                    defaultTier = state.defaultTier,
                    providerKeyStatus = state.providerKeyStatus,
                    modelCatalog = modelCatalog,
                    keyValidator = keyValidator,
                    onProviderSelected = { p -> providerVm.dispatch(ProviderIntent.SetProvider(p)) },
                    onProviderKeySaved = { p, k ->
                        providerVm.dispatch(ProviderIntent.SaveProviderKey(p, k))
                    },
                    onProviderKeyRemoved = { p ->
                        providerVm.dispatch(ProviderIntent.RemoveProviderKey(p))
                    },
                    onDefaultTierSelected = { t ->
                        providerVm.dispatch(ProviderIntent.SetDefaultTier(t))
                    },
                    getModelOverride = { p, t -> modelPrefsRepo.overrideFor(p, t) },
                    onModelOverrideSelected = { p, t, id ->
                        providerVm.dispatch(ProviderIntent.SetModelForTier(p, t, id))
                    },
                    onOpenConsole = platform.onOpenUrl,
                    onBack = { navigationVm.dispatch(NavigationIntent.Navigate(Screen.Settings)) },
                )
            }

            Screen.Appearance -> AppearanceScreen(
                selectedPalette = state.themePrefs.palette,
                selectedMode = state.themePrefs.mode,
                onPaletteSelected = { p -> themeVm.dispatch(ThemeIntent.SetPalette(p)) },
                onModeSelected = { m -> themeVm.dispatch(ThemeIntent.SetThemeMode(m)) },
                onBack = { navigationVm.dispatch(NavigationIntent.Navigate(Screen.Settings)) },
            )

            Screen.Usage -> UsageScreen(
                onBack = { navigationVm.dispatch(NavigationIntent.Navigate(Screen.Settings)) },
            )

            Screen.Personas -> PersonasScreen(
                onBack = { navigationVm.dispatch(NavigationIntent.Navigate(Screen.Chat)) },
                onStartCreator = { kind ->
                    val ck = when (kind) {
                        PersonaKind.Voice -> CreatorKind.PersonaVoice
                        PersonaKind.Role -> CreatorKind.PersonaRole
                        PersonaKind.Custom -> CreatorKind.PersonaVoice
                    }
                    creatorVm.dispatch(CreatorIntent.StartCreator(ck))
                },
            )
        }
    }
}
