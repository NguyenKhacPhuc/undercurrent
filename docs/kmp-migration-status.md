# KMP migration — current status

Living document. Updated each session. Pair with
[`kmp-migration-playbook.md`](kmp-migration-playbook.md) for the
per-feature recipe.

## Foundation — DONE

All seven foundation modules are migrated and verified to compile
on **both Android and iOS** targets. The gateway layer (commonMain
interfaces + Android impls + iOS stubs) landed in the follow-up
session, so every cross-cutting Weft dependency now has a
KMP-friendly contract.

| Module | What's in it |
|---|---|
| ✅ `:core:model` | AppEffect, ThemeMode, ThemePrefs, AppPalette, **ProviderKind**, **ModelTier**, **Persona / PersonaKind / BuiltInPersonas**, **MiniApp** |
| ✅ `:core:design-system` | UndercurrentColors, UndercurrentShapes, UndercurrentTypography, UndercurrentTheme, Palettes (8 color tables + `AppPalette.colors(dark)` extension) |
| ✅ `:core:ui` | LoadingPlaceholder, ScreenScaffold, ScaffoldTextAction, TokenDivider, SectionLabel |
| ✅ `:shared` | **AgentEngine** + ChatChunk/AgentState/StubAgentEngine. **Gateway layer**: KeyVaultGateway, OAuthGateway, ConversationStoreGateway, MemoryStoreGateway, TraceStoreGateway, UsageGateway, ModelCatalog, SpeechGateway, UiBridgeGateway (with mirror types — ConversationSummary, MemoryEntry/Scope, AgentTrace/LlmCallTrace/ToolCallTrace + status enums, UsageTotals, ModelInfo/ModelPool, VoiceState, ComponentNode/UiRenderEvent, OAuthConfig/OAuthTokens/OAuthResult). iOS stubs for all nine. |
| ✅ `:data:weft` (Android-only) | WeftAgentEngine + **nine gateway impls** — WeftKeyVaultGateway, WeftOAuthGateway, WeftConversationStoreGateway, WeftMemoryStoreGateway, WeftTraceStoreGateway, WeftUsageGateway, WeftModelCatalog, AndroidSpeechGateway, WeftUiBridgeGateway. |
| ✅ `:data:datastore` | createPreferencesDataStore (KMP factory + Android/iOS actuals), ThemeRepository, OnboardingRepository, PersonaRepository, IntegrationsRepository, MiniAppsRepository, ProviderPrefsRepository, ModelPrefsRepository — **7 repos total** |
| ✅ `:data:sqldelight` | UndercurrentDatabase (Records.sq schema), expect class DatabaseDriverFactory + Android (AndroidSqliteDriver) / iOS (NativeSqliteDriver) actuals |

## Features — 17 of 17 done

| Feature | Status | Blocker / notes |
|---|---|---|
| ✅ `:feature:settings` | **DONE** | Proof-of-pattern. |
| ✅ `:feature:conversations` | **DONE** | Recipe A. |
| ✅ `:feature:memories` | **DONE** | Recipe A. |
| ✅ `:feature:traces` | **DONE** | Recipe A. Full detail view + clipboard formatters. |
| ✅ `:feature:usage` | **DONE** | Recipe A. |
| ✅ `:feature:theme` | **DONE** | Stateless AppearanceScreen. |
| ✅ `:feature:onboarding` | **DONE** | `catalogFor(provider)` lifted to `modelCountFor` lambda. |
| ✅ `:feature:personas` | **DONE** | `CreatorKind` dep replaced with `(PersonaKind) -> Unit` lambda. |
| ✅ `:feature:miniapps` | **DONE** | `TreeRenderer` hoisted to `treePreview` lambda. |
| ✅ `:feature:integrations` | **DONE** | OAuthGateway + IntegrationsRepository. Integration mirror uses commonMain OAuthConfig. |
| ✅ `:feature:keypaste` | **DONE** | New `KeyValidationGateway` added; pure-data helpers (`apiConsoleUrl`, `signupHint`, `hostName`, `keyPlaceholder`) moved to `:core:model`. `openInBrowser` lifted to `onOpenConsole` lambda. Material icons replaced with "Show"/"Hide" labels. |
| ✅ `:feature:voice` | **DONE** | WaveformBars in commonMain reading from `SpeechGateway.rmsdB`. Old Android-only VoiceRecognizer replaced by AndroidSpeechGateway (landed with gateway batch). |
| ✅ `:feature:maps` | **DONE** | No UI to migrate — feature is just `ShowLocationOnMapTool` (a Weft tool). Moved to `:data:weft/.../tools/` since Weft tools must be androidMain. |
| ✅ `:feature:creator` | **DONE** | CreatorSession + CreatorKind + CreatorScreen in commonMain. Tree-rendering body hoisted to a `body: @Composable () -> Unit` lambda so host can wire Weft's TreeRenderer + ComposeUiBridge. `CreatorTools` (create_persona, create_mini_app) stays in `app/` for now — depends on un-migrated `Screen` / `NavigationChannel`; moves to `:data:weft` when navigation lands. |
| ✅ `:feature:providers` | **DONE** | Largest screen yet (~720 LOC). Switched from Koog `LLModel` to `ModelInfo` mirror; from `catalogFor`/`defaultPoolFor` to `ModelCatalog` gateway; from inline `validateKey` to `KeyValidationGateway`; from CCT `openInBrowser` to `onOpenConsole` lambda. `TipBox` promoted to `:core:ui`. Material icons (Visibility, ArrowDropDown, KeyboardArrowRight) → Unicode glyphs. |
| ✅ `:feature:chat` | **DONE** | The big one — 2472 LOC across 6 files. ChatScreen + DegradedModeBanner + NotificationsPermissionBanner + AddToChatSheet + MarkdownText + AgentSelector + DisplayMessage/ToolInfo + DegradedMode/SkillSummary/AgentOption mirrors all in commonMain. SpeechGateway replaces VoiceRecognizer + Android permission flow. `LocalClipboardManager` → `onCopyText` lambda. CCT `openInBrowser` → `onOpenUrl` lambda (passed through to MarkdownText links). CircuitBreaker → `DegradedMode` mirror state. `SkillRegistry` → `List<SkillSummary>?`. `AgentSelector` re-implemented in commonMain (was `:android-compose-defaults`-only). Material icons-extended (Menu, MoreVert, ArrowUpward, AutoAwesome, Mic, Add) → Unicode glyphs (☰, ⋮, ↑, ✦, ●, +). |
| ✅ `:feature:navigation` | **DONE** | `Screen` sealed interface + `NavigationChannel` lifted into `:core:navigation/commonMain` (made `public`). The five `Open*Tool` Weft tools moved to `:data:weft/.../tools/NavigationTools.kt`. `CreatePersonaTool` + `CreateMiniAppTool` also moved to `:data:weft/.../tools/CreatorTools.kt` now that their `Screen` / `NavigationChannel` / `PersonaRepository` / `MiniAppsRepository` deps are reachable from a commonMain-aware module. |

## Patterns established

Every remaining feature follows one of these recipes:

### Recipe A — feature with Weft-store reads only
Examples: `:feature:conversations`, `:feature:memories`, `:feature:traces`, `:feature:usage`

1. Define a `<Feature>Gateway` interface in `:shared/commonMain/.../gateway/` exposing the read operations the feature needs (list/get/delete/observe). Use KMP-friendly types from :core:model or local mirrors.
2. Add an Android implementation in `:data:weft/src/main/kotlin/.../` wrapping the corresponding Weft store (`runtime.conversationStore`, `runtime.memoryStore`, etc.).
3. Add an iOS stub in `:shared/iosMain/.../gateway/` that returns empty flows / throws on writes.
4. Move the feature's `<Feature>ViewModel` to `:feature:<name>/commonMain` — constructor takes the gateway interface.
5. Move the `<Feature>Screen` to `:feature:<name>/commonMain` — replace `internal` with `public`, swap any Weft imports for the gateway types.

### Recipe B — feature with DataStore prefs only
Examples: `:feature:theme`, `:feature:onboarding`, parts of `:feature:miniapps` and `:feature:personas`

1. Repository already migrated to `:data:datastore/commonMain`. Pull the ViewModel + Screen forward.
2. Constructor takes the migrated `<Name>Repository` directly. No gateway needed.
3. Move Screen + ViewModel to `:feature:<name>/commonMain`.

### Recipe C — feature with OAuth / KeyVault / agent tools
Examples: `:feature:integrations`, `:feature:keypaste`, `:feature:creator`, `:feature:voice`

1. Define an interface in `:shared` for the cross-cutting concern (`OAuthGateway`, `KeyVaultGateway`, `SpeechGateway`).
2. Android impl in `:data:weft` delegates to the Weft contract.
3. iOS stub in `:shared/iosMain` returns "not supported".
4. Migrate ViewModel + Screen consuming the interface.

### Recipe D — `:feature:chat`
The single hardest one. Bigger writeup needed before tackling it.
Consumes AgentEngine (already defined) but also needs UIUpdate
plumbing (the `ui_render` payload pipeline) and the streaming
chunk → UI translation. Plan a focused session.

## Cross-feature gateway layer — DONE

All nine cross-cutting interfaces are defined in `:shared/commonMain`
with mirror types, backed by Android impls in `:data:weft`, and
stubbed in `:shared/iosMain`. Both targets compile.

| Interface | Used by | Android impl |
|---|---|---|
| `KeyVaultGateway` | keypaste, providers | `WeftKeyVaultGateway` (delegates to Weft `KeyVault` + provider-alias map) |
| `OAuthGateway` | integrations | `WeftOAuthGateway` (wraps `OAuthClient` + `OAuthTokenStore`, translates `OAuthResult` sealed variants) |
| `ConversationStoreGateway` | conversations, chat | `WeftConversationStoreGateway` (search/delete/clear-all over `ConversationStore`) |
| `MemoryStoreGateway` | memories, chat | `WeftMemoryStoreGateway` (read-only flow + delete/clear over `MemoryStore`) |
| `TraceStoreGateway` | traces | `WeftTraceStoreGateway` (full mirror of AgentTrace/LlmCallTrace/ToolCallTrace + setFeedback) |
| `UsageGateway` | usage, providers | `WeftUsageGateway` (totals flow over `UsageStore`) |
| `ModelCatalog` | providers, onboarding | `WeftModelCatalog` (projects Koog `LLModel` → lossy `ModelInfo`; preserves `hasVision` / `hasTools` capability bits) |
| `SpeechGateway` | voice | `AndroidSpeechGateway` (wraps `SpeechRecognizer` directly — no Weft dependency) |
| `UiBridgeGateway` | miniapps, chat | `WeftUiBridgeGateway` (`snapshotFlow` over `ComposeUiBridge.lastUpdate`, filters to RenderTree events) |

Mirror types live alongside their gateway in `shared/commonMain/.../gateway/`.
Wire format compatible with the Weft originals — cached `ui_render`
JSON round-trips cleanly between the mirror `ComponentNode` and
`dev.weft.contracts.ComponentNode`.

## Host-app shell move — DONE (assembleDebug green)

`:androidApp` now compiles and assembles a debug APK end-to-end via
`./gradlew :androidApp:assembleDebug`. The shell move landed:

- `UndercurrentApp` (Koin boot), `MainActivity` (FragmentActivity +
  OAuth deep-link plumbing + edge-to-edge), `App()` Composable +
  `ScreenRouter` + `ChatRoute` — all in `:androidApp/src/main/kotlin/.../core/`.
- `AppState` / `AppIntent` / `AppPreamble` — rewritten to use the
  commonMain mirror types (`ProviderKind` / `ModelTier` from
  `:core:model`); AppStore translates to Weft enums at the runtime
  boundary via `toWeft()` / `toMirror()` helpers.
- `AppStore` (state machine, agent loop, model-pool override
  resolution) — Android-only, references Weft types directly.
- `AppModule` (Koin DI) — wires 7 DataStore-backed repos with named
  qualifiers, 10 gateways (`KeyVault`, `OAuth`, `Conversation`,
  `Memory`, `Trace`, `Usage`, `ModelCatalog`, `Speech`, `UiBridge`,
  `KeyValidation`), the `UndercurrentDatabase` via
  `:data:sqldelight`, the `WeftRuntime` with all extra tools
  registered, and the 7 per-screen ViewModels.
- `SqlDelightDataSource` rewritten against `UndercurrentDatabase`
  (renamed from `AppDatabase`); `InMemoryDataSource` dropped (unused).
- `SetThemePaletteTool` + `SetThemeModeTool` rewritten in
  `:androidApp/.../tools/` against the migrated `ThemeRepository`.
- `AppDrawer` + `UrlLauncher` + 40+ component files copied to
  `:androidApp/.../{ui,components}/` with bulk import rewrites
  (`dev.weft.undercurrent.theme.*` → `dev.weft.undercurrent.core.designsystem.*` etc.).
- `AndroidApplicationConventionPlugin` now sets
  `kotlin { compilerOptions { jvmTarget = JVM_17 } }` to match the
  Java target — fixes the inconsistent-JVM-target error.

### Follow-ups from the shell move — DONE

1. **MiniAppsScreen treePreview** — wires `TreeRenderer` against the
   `weftUi.componentRegistry` over the cached JSON; falls back to the
   "(preview)" placeholder when the JSON can't be parsed. Taps on the
   card always route to `onTap` (component events are swallowed).
2. **TraceViewerScreen onExportTrace** — `AppIntent.ExportTrace` now
   carries `traceId: String`; `AppStore.handleExportTrace` re-resolves
   the Weft `AgentTrace` from `runtime.traceStore.traces`. Surfaces an
   `AppEffect.Error` when the id no longer matches.
3. **Chat copy-text wiring** — `onCopyText` calls Android's
   `ClipboardManager.setPrimaryClip` with the message body.
4. **Old `app/`** — deleted. Source preserved in git history (commit
   `412a2cd` was the last working state before the shell move).

## App() → :composeApp/commonMain — DONE

The orchestrator layer is now genuinely shared:

| New location | What's there |
|---|---|
| `:composeApp/commonMain/.../app/AppState.kt` | Mirror-typed state vocabulary. `agent: WeftAgent?` was replaced by `agentReady: Boolean` + `currentConversationId: String?`. Adds `providerKeyStatus: Map<ProviderKind, String>` so the providers screen doesn't need direct keyvault access. |
| `:composeApp/commonMain/.../app/AppIntent.kt` | Mirror-typed intents. `UIUpdate?` swapped for `UiRenderEvent?` from `UiBridgeGateway`. `InvokeMiniApp` carries `cachedRenderTreeJson: String?` so the seed-then-invoke flow is one dispatch. |
| `:composeApp/commonMain/.../app/AppStore.kt` | Interface — exposes only mirror types (`SkillSummary` / `DisplayMessage` / `AppState`). Skills are projected to `List<SkillSummary>` at the boundary. `modelPrefsRepo` removed from the interface; ScreenRouter injects it directly via Koin. |
| `:composeApp/commonMain/.../app/App.kt` | Top-level composable — theme wrapper, snackbar host, permission-needed dialog overlay, plus the screen router. |
| `:composeApp/commonMain/.../app/ScreenRouter.kt` | Switches over `Screen`. KMP-clean routes (Settings, Appearance, KeyPaste, Onboarding, Providers, Personas, Memories, Traces, Usage, Integrations, Conversations) render here directly. |
| `:composeApp/commonMain/.../app/PlatformAdapter.kt` | Holds the substrate-coupled routes as `@Composable () -> Unit` lambdas (`chatRoute` / `renderedTreeRoute` / `creatorRoute` / `miniAppsRoute`) plus OS bridges (`onOpenUrl` / `onCopyText` / `onRestartProcess` / `onOpenAppDetailsSettings` / `onOpenSaveDialog`). |

### Android side

`:androidApp/.../core/WeftAppStore.kt` is the Weft-using impl of the
commonMain `AppStore`. Subscribes to `UiBridgeGateway.renderEvents`
internally (no more `snapshotFlow { uiBridge.lastUpdate }` in
MainActivity). `MainActivity` is now ~370 LOC (down from 850):
boots Koin, plumbs OAuth deep links, constructs the
`PlatformAdapter` with the Android-only screen wiring (`ChatRoute`
with drawer, the substrate `AgentRenderedTreeScreen`,
`TreeRenderer`-backed mini-app preview, `CreatorScreen` body, the
`SaveAsMiniAppDialog` at the App root), then calls
`App(store, platform)`.

AppStore is bound via `single<AppStore> { WeftAppStore(...) }` (not
`viewModel<...>`) because the commonMain interface can't extend
`androidx.lifecycle.ViewModel` — iOS wouldn't compile. WeftAppStore
itself still extends ViewModel internally for `viewModelScope`.

### iOS side

`:composeApp/iosMain/.../app/IosAppStore.kt` — stub impl emitting a
fixed loading state. `IosPlatformAdapter.kt` — placeholder
composables for the substrate-coupled routes; OS bridges are no-ops
until the iOS agent runtime lands. Both
`:composeApp:compileKotlinIosArm64` and
`:composeApp:compileKotlinIosSimulatorArm64` build green.

## iOS — Kotlin side ready, Xcode shell pending

The Kotlin side of iOS launch is now complete. `ComposeApp.framework`
links successfully for both `iosArm64` + `iosSimulatorArm64`.

| New file | What it does |
|---|---|
| `:composeApp/iosMain/.../app/MainViewController.kt` | Top-level `fun MainViewController(): UIViewController` returning a `ComposeUIViewController { App(...) }`. Resolves `AppStore` from Koin and constructs `iosPlatformAdapter()`. This is the symbol Swift calls. |
| `:composeApp/iosMain/.../app/IosKoinModule.kt` | `val iosAppModule` — binds 7 named DataStores (via `createPreferencesDataStore(name)`), 7 repos, `DatabaseDriverFactory` + `UndercurrentDatabase`, the 11 stub gateways from `:shared/iosMain`, `IosAppStore`, and the 7 per-screen ViewModels. No `androidContext` / `WeftRuntime`. |
| `:composeApp/iosMain/.../app/InitKoin.kt` | `fun initKoin()` — `startKoin { modules(iosAppModule) }`. Swift calls this once at launch. |
| `IosAppStore.dispatch` (updated) | Handles `Resume` → `Screen.Onboarding` and `Navigate(...)` so iOS users see real UI on first frame instead of the loading placeholder. Other intents still no-op until a real iOS agent lands. |

### Build verification

```
./gradlew :composeApp:linkDebugFrameworkIosArm64 \
          :composeApp:linkDebugFrameworkIosSimulatorArm64
# → BUILD SUCCESSFUL; framework binaries land under
#   composeApp/build/bin/{iosArm64,iosSimulatorArm64}/debugFramework/ComposeApp.framework
```

Linker warnings (`i:`) about `kotlinx.datetime/Instant` symbols in
Material3 are a known issue — kotlinx-datetime is pinned to 0.7.1
(per the AWS-SDK transitive constraint, see CLAUDE.md note) and
Material3's iOS `KotlinxDatetimeCalendarModel` calls 0.6.x-style
APIs that 0.7.x renamed. Material3's `DatePicker` will crash on iOS
if invoked; nothing else is affected. Track upstream
compose-multiplatform for the fix.

### To actually launch (Xcode side — needs Mac + Xcode)

1. **Create `iosApp/` Xcode project**: File → New → Project → iOS App,
   "Interface: SwiftUI", saved at repo root as `iosApp/`.
2. **Swift `@main` App**:
   ```swift
   import SwiftUI
   import ComposeApp

   @main
   struct iOSApp: App {
       init() { Main_iosKt.doInitKoin() }
       var body: some Scene {
           WindowGroup {
               ComposeView().ignoresSafeArea()
           }
       }
   }
   struct ComposeView: UIViewControllerRepresentable {
       func makeUIViewController(context: Context) -> UIViewController =
           Main_iosKt.MainViewController()
       func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
   }
   ```
   (`Main_iosKt` / `doInitKoin` names follow the Kotlin/Native ObjC
   mangling — verify with `nm ComposeApp.framework/ComposeApp | grep -i init`.)
3. **Embed the framework**: Xcode → Build Phases → "+ Run Script Phase":
   ```
   ./gradlew :composeApp:embedAndSignAppleFrameworkForXcode
   ```
   Add `$(SRCROOT)/../composeApp/build/xcode-frameworks/$(CONFIGURATION)/$(SDK_NAME)`
   to Framework Search Paths.
4. **Build & run** — `Cmd-R` on an iOS Simulator scheme. Expected:
   app boots → Koin init → `App()` mounts → `Resume` dispatches →
   lands at `Screen.Onboarding`. Provider picker is fully functional
   (`ModelCatalog` stub returns empty, but the screen renders).
   Stepping into Chat shows the "Chat — coming to iOS" placeholder.

### Still on the iOS roadmap

- **iOS OS-bridge wiring** — `iosPlatformAdapter()`'s `onOpenUrl` /
  `onCopyText` / `onOpenAppDetailsSettings` are still no-ops. Wire
  to `UIApplication.shared.open(_:)` / `UIPasteboard.general.string` /
  `UIApplication.openSettingsURLString` once the Xcode shell is
  proven booting.
- **iOS agent runtime** — `IosAppStore` is a stub. A real iOS
  `AppStore` (Weft Swift client, or a different agent SDK on iOS)
  is the next major chunk.
- **iOS Material3 DatePicker** — broken at the kotlinx-datetime
  version-pin level. Avoid in commonMain screens or guard with a
  `expect/actual` shim.

## (Stale) Recommended next session — superseded by above

**Every feature is migrated.** What's left is the host-app shell move
— big in line count but mostly mechanical now that the gateways +
feature modules are stable.

1. **`:androidApp` + `:composeApp` wiring** — the orchestrator layer.
   ~1 day.
   - **AppState / AppIntent / AppEffect / AppPreamble** (450 LOC of
     state machine vocabulary) → these reference Weft types directly
     (`WeftAgent`, `UIUpdate`, `AgentTrace`, `WeftSystemPromptDefaults`).
     They can either (a) stay co-located with AppStore in `:androidApp`
     or (b) shift to commonMain gateway mirrors. Pragmatic call: keep
     them with AppStore in `:androidApp` since AppStore is the only
     consumer and the rewrite to gateway mirrors isn't load-bearing.
   - **AppStore** (957 LOC) — the reducer + agent loop + every
     gateway consumer. Moves to `:androidApp/src/main/kotlin/.../`
     verbatim (rewrite imports: old `dev.weft.undercurrent.features.*`
     → new `dev.weft.undercurrent.feature.*` + `:core:navigation`
     for `Screen` / `NavigationChannel`).
   - **AppModule** (473 LOC, Koin DI) → `:androidApp/.../di/`.
     This is the binding layer for every gateway. Bind:
     `KeyVaultGateway` → `WeftKeyVaultGateway`, `OAuthGateway` →
     `WeftOAuthGateway`, … (9 gateways + the 10th
     `KeyValidationGateway` → `WeftKeyValidationGateway` +
     `AgentEngine` → `WeftAgentEngine` + `AndroidSpeechGateway`).
   - **MainActivity** (846 LOC) — Android entrypoint with
     OAuth deep-link plumbing + permission launchers. Moves verbatim.
   - **UndercurrentApp** (36 LOC) — Application class. Moves verbatim.
   - **The `App()` composable + screen routing** — extracts to
     `:composeApp/commonMain` parameterized by every feature module's
     screen factory. The host wires which screen ID → which feature
     module's screen Composable. iOS shell gets the same `App()`
     dispatched against a different (stub-backed) DI graph.
2. **iOS shell** — Xcode project, SwiftUI scene hosting
   `ComposeApp.framework`. ~½ day.
3. **Delete `app/`** — once everything builds through the new modules.

Estimated total remaining: ~1.5 focused days.

## Patterns learned (Recipes A + B + C)

- **`:shared` brings `:core:model` transitively.** Set up in the gateway commit; feature modules add `implementation(projects.shared)` and pick up `ProviderKind` / `ModelTier` automatically.
- **`org.koin.compose.viewmodel.koinViewModel`** is the commonMain entry point. Don't import `org.koin.androidx.compose.koinViewModel` — that's the Android-only shim from the old app.
- **`androidx.lifecycle.ViewModel` + `viewModelScope`** are KMP-available through koin-compose-viewmodel's transitive deps. No special imports needed.
- **`java.util.Date` / `SimpleDateFormat` / `java.text.*` / `java.time.*`** → kotlinx.datetime + `kotlin.time.Clock` per CLAUDE.md note. Add `implementation(libs.kotlinx.datetime)` per feature, opt-in via `@OptIn(ExperimentalTime::class)` when using `kotlin.time.Instant`.
- **`String.format("%.Nf", v)` doesn't exist in commonMain stdlib.** Write a small `formatDecimal(v, n)` helper (see `feature/usage`). One-decimal-only variants also acceptable inline.
- **`Map.toSortedMap()` doesn't exist in commonMain stdlib.** Use `entries.sortedBy { it.key }` (ISO dates sort lexicographically = chronologically).
- **Lift Android-only Composable dependencies to lambda parameters** (Recipe B insight). When a screen needs an Android-only Composable like `TreeRenderer` or a static Android-only call like `catalogFor()`, take the rendering / lookup as a constructor lambda. The host wires it from the Android nav glue; the screen stays commonMain. See `feature/miniapps` (`treePreview`) + `feature/onboarding` (`modelCountFor`).
- **When a screen depends on a sibling feature module that hasn't migrated yet** (e.g. `:feature:personas` used `:feature:creator`'s `CreatorKind`), lift to a generic callback — `(PersonaKind) -> Unit` instead of `(CreatorKind) -> Unit`. The host translates. Avoids cross-feature ordering constraints.
- **Material `icons-extended` isn't in CMP commonMain by default.** Replace with Unicode glyphs: `▾` (ArrowDropDown), `›` (KeyboardArrowRight), `←` (back), `×` (Close), plus plain "Show"/"Hide" text for password toggles. Matches the design-language conventions of the existing migrated screens.
- **Promote shared screen helpers to `:core:ui` lazily — when the second feature would import them.** `TipBox` lived in `app/ui/` until `:feature:providers` migration needed it; promoted then. Same approach if `:feature:chat` needs anything more.
- **Pure-data per-provider helpers** (`apiConsoleUrl`, `signupHint`, `hostName`, `keyPlaceholder`) belong in `:core:model` as `ProviderKind` extensions — both `:feature:keypaste` and `:feature:providers` consume them.
- **When a Weft Compose component is small** (like `AgentSelector` — ~30 lines of AssistChip + DropdownMenu), re-implementing it in commonMain is cleaner than hoisting through a lambda. Mirror the wire-format types (e.g. `AgentOption`) and the chat surface stays self-contained.
- **CircuitBreaker / sealed-state-with-timing hosts pass naturally as snapshot mirrors.** `DegradedMode(openedAtEpochMs, cooldownMs)` is enough for a count-down banner — no need to plumb the full sealed-class state, since the screen only renders the open variant. Use `kotlin.time.Clock.System.now()` instead of `System.currentTimeMillis()` for the timer tick.

## What's solid right now

- All seven foundation modules compile clean on both Android + iOS.
- The Weft composite build resolves end-to-end through
  `includeBuild("../weft")`.
- Convention plugins (build-logic/) are stable; adding new feature
  modules is a one-line plugin application + a copy-paste of the
  feature dir structure.
- DataStore + SQLDelight + Compose Multiplatform + Koin + Kotlin
  Time all work as expected in commonMain.
- One feature migrated end-to-end proves the pattern works.

## What's stale / paused

- `app/` is no longer in `settings.gradle.kts` and isn't building.
  Its source is reference-only until the migration finishes and it
  gets deleted.
- The `:androidApp` and `:composeApp` modules are scaffold-only
  (empty `commonMain/`). Nothing runs end-to-end yet on Android —
  only individual module compiles are green.
- `iosApp/` isn't created. No iOS Xcode project until `:composeApp`
  has real content.
