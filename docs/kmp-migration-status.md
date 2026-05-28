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

## Recommended next session

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
